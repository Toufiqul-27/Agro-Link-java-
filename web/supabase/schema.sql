-- AgroLink BD Web MVP — execute in Supabase SQL Editor on a NEW project.
-- Do not run blindly over an existing production schema. No demo users are inserted.
-- Public profiles intentionally contain NO phone, NID or private contact details.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS public.profiles (
  id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name text NOT NULL CHECK (char_length(full_name) BETWEEN 2 AND 100),
  district text NOT NULL CHECK (char_length(district) BETWEEN 2 AND 80),
  role text NOT NULL CHECK (role IN ('farmer', 'buyer', 'transporter')),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION public.on_auth_user_created()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $$
BEGIN
  INSERT INTO public.profiles(id, full_name, district, role)
  VALUES (
    NEW.id,
    left(coalesce(nullif(trim(NEW.raw_user_meta_data->>'full_name'), ''), 'New member'), 100),
    left(coalesce(nullif(trim(NEW.raw_user_meta_data->>'district'), ''), 'Other'), 80),
    CASE WHEN NEW.raw_user_meta_data->>'role' IN ('farmer','buyer','transporter')
      THEN NEW.raw_user_meta_data->>'role' ELSE 'buyer' END
  ) ON CONFLICT(id) DO NOTHING;
  RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.on_auth_user_created();

CREATE TABLE IF NOT EXISTS public.listings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  farmer_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT,
  crop_name text NOT NULL CHECK (char_length(crop_name) BETWEEN 2 AND 100),
  category text NOT NULL CHECK (category IN ('Vegetables','Fruits','Rice & Grains','Spices','Other')),
  district text NOT NULL CHECK (char_length(district) BETWEEN 2 AND 80),
  description text CHECK (char_length(description) <= 1000),
  price_per_kg numeric(12,2) NOT NULL CHECK (price_per_kg > 0),
  stock_kg integer NOT NULL CHECK (stock_kg >= 0),
  status text NOT NULL DEFAULT 'active' CHECK (status IN ('active','paused')),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS listings_active_created_idx
  ON public.listings (created_at DESC) WHERE status = 'active';

CREATE TABLE IF NOT EXISTS public.orders (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id uuid NOT NULL REFERENCES public.listings(id) ON DELETE RESTRICT,
  buyer_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT,
  quantity_kg integer NOT NULL CHECK (quantity_kg > 0),
  total_bdt numeric(14,2) NOT NULL CHECK (total_bdt >= 0),
  status text NOT NULL DEFAULT 'pending'
    CHECK (status IN ('pending','confirmed','dispatched','cancelled')),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS orders_buyer_idx ON public.orders (buyer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS orders_listing_idx ON public.orders (listing_id);

CREATE TABLE IF NOT EXISTS public.trucks (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT,
  origin text NOT NULL CHECK (char_length(origin) BETWEEN 2 AND 100),
  destination text NOT NULL CHECK (char_length(destination) BETWEEN 2 AND 100),
  departure_date date NOT NULL,
  capacity_kg integer NOT NULL CHECK (capacity_kg > 0),
  reserved_kg integer NOT NULL DEFAULT 0 CHECK (reserved_kg >= 0 AND reserved_kg <= capacity_kg),
  price_per_kg numeric(12,2) NOT NULL CHECK (price_per_kg > 0),
  status text NOT NULL DEFAULT 'active' CHECK (status IN ('active','closed')),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS trucks_active_departure_idx
  ON public.trucks (departure_date) WHERE status = 'active';

CREATE TABLE IF NOT EXISTS public.truck_bookings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  truck_id uuid NOT NULL REFERENCES public.trucks(id) ON DELETE RESTRICT,
  user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT,
  weight_kg integer NOT NULL CHECK (weight_kg > 0),
  total_bdt numeric(14,2) NOT NULL CHECK (total_bdt >= 0),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS bookings_user_idx ON public.truck_bookings(user_id);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trucks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.truck_bookings ENABLE ROW LEVEL SECURITY;

-- Profile name, role and district are deliberately public to support market transparency.
CREATE POLICY profiles_public_read ON public.profiles FOR SELECT USING (true);

CREATE POLICY listings_public_read ON public.listings FOR SELECT
  USING (status = 'active' OR farmer_id = auth.uid());
CREATE POLICY listings_farmer_insert ON public.listings FOR INSERT TO authenticated
  WITH CHECK (
    farmer_id = auth.uid() AND
    EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'farmer')
  );
-- Listing quantities must be mutated by RPC only after creation to avoid bypassing orders.
-- No direct UPDATE / DELETE policy.

CREATE POLICY orders_participants_read ON public.orders FOR SELECT TO authenticated
  USING (
    buyer_id = auth.uid() OR
    EXISTS (SELECT 1 FROM public.listings WHERE listings.id = listing_id
      AND listings.farmer_id = auth.uid())
  );
-- No direct INSERT / UPDATE / DELETE policy on orders.

CREATE POLICY trucks_public_read ON public.trucks FOR SELECT
  USING (status = 'active' OR owner_id = auth.uid());
CREATE POLICY trucks_driver_insert ON public.trucks FOR INSERT TO authenticated
  WITH CHECK (owner_id = auth.uid() AND
    EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'transporter'));
-- No direct UPDATE / DELETE policy on trucks to prevent tampering with reservations.

CREATE POLICY bookings_participants_read ON public.truck_bookings FOR SELECT TO authenticated
  USING (
    user_id = auth.uid() OR
    EXISTS (SELECT 1 FROM public.trucks WHERE trucks.id = truck_id
      AND trucks.owner_id = auth.uid())
  );
-- No direct write policy for truck_bookings.

CREATE OR REPLACE FUNCTION public.place_order(
  p_listing_id uuid, p_quantity_kg integer
) RETURNS uuid LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $$
DECLARE
  v_buyer uuid := auth.uid();
  v_listing public.listings%ROWTYPE;
  v_id uuid;
BEGIN
  IF v_buyer IS NULL OR NOT EXISTS (
    SELECT 1 FROM public.profiles WHERE id=v_buyer AND role='buyer'
  ) THEN RAISE EXCEPTION 'Buyer account required'; END IF;
  IF p_quantity_kg IS NULL OR p_quantity_kg <= 0 OR p_quantity_kg > 100000
    THEN RAISE EXCEPTION 'Invalid quantity'; END IF;

  SELECT * INTO v_listing FROM public.listings
    WHERE id=p_listing_id FOR UPDATE;
  IF NOT FOUND OR v_listing.status <> 'active' OR
    v_listing.stock_kg < p_quantity_kg OR v_listing.stock_kg = 0
    THEN RAISE EXCEPTION 'Crop unavailable or insufficient stock'; END IF;
  IF v_listing.farmer_id = v_buyer THEN
    RAISE EXCEPTION 'Cannot buy your own listing'; END IF;

  UPDATE public.listings SET stock_kg=stock_kg - p_quantity_kg WHERE id=p_listing_id;
  INSERT INTO public.orders(listing_id,buyer_id,quantity_kg,total_bdt)
  VALUES (p_listing_id,v_buyer,p_quantity_kg,v_listing.price_per_kg*p_quantity_kg)
  RETURNING id INTO v_id;
  RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.change_order_status(
  p_order_id uuid, p_status text
) RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $$
DECLARE
  v_order public.orders%ROWTYPE;
  v_farmer uuid;
BEGIN
  SELECT * INTO v_order FROM public.orders WHERE id=p_order_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Order not found'; END IF;
  SELECT farmer_id INTO v_farmer FROM public.listings WHERE id=v_order.listing_id;

  IF p_status='cancelled' AND v_order.status='pending'
     AND v_order.buyer_id=auth.uid() THEN
    UPDATE public.orders SET status='cancelled' WHERE id=p_order_id;
    UPDATE public.listings SET stock_kg=stock_kg+v_order.quantity_kg
      WHERE id=v_order.listing_id;
  ELSIF v_farmer=auth.uid() AND
     ((p_status='confirmed' AND v_order.status='pending')
      OR (p_status='dispatched' AND v_order.status='confirmed')) THEN
    UPDATE public.orders SET status=p_status WHERE id=p_order_id;
  ELSE RAISE EXCEPTION 'Order status transition not permitted';
  END IF;
END;
$$;

CREATE OR REPLACE FUNCTION public.reserve_truck(
  p_truck_id uuid, p_weight_kg integer
) RETURNS uuid LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $$
DECLARE
  v_user uuid := auth.uid();
  v_truck public.trucks%ROWTYPE;
  v_id uuid;
BEGIN
  IF v_user IS NULL OR NOT EXISTS (
    SELECT 1 FROM public.profiles WHERE id=v_user AND role IN ('buyer','farmer')
  ) THEN RAISE EXCEPTION 'Farmer or buyer account required'; END IF;
  IF p_weight_kg IS NULL OR p_weight_kg <= 0 OR p_weight_kg > 100000
    THEN RAISE EXCEPTION 'Invalid weight'; END IF;
  SELECT * INTO v_truck FROM public.trucks WHERE id=p_truck_id FOR UPDATE;
  IF NOT FOUND OR v_truck.status<>'active' OR
    v_truck.departure_date<CURRENT_DATE OR
    v_truck.reserved_kg+p_weight_kg>v_truck.capacity_kg THEN
    RAISE EXCEPTION 'Truck unavailable or not enough room'; END IF;
  UPDATE public.trucks SET reserved_kg=reserved_kg+p_weight_kg WHERE id=p_truck_id;
  INSERT INTO public.truck_bookings(truck_id,user_id,weight_kg,total_bdt)
  VALUES (p_truck_id,v_user,p_weight_kg,v_truck.price_per_kg*p_weight_kg)
  RETURNING id INTO v_id;
  RETURN v_id;
END;
$$;

REVOKE ALL ON FUNCTION public.place_order(uuid,integer) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.change_order_status(uuid,text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.reserve_truck(uuid,integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.place_order(uuid,integer) TO authenticated;
GRANT EXECUTE ON FUNCTION public.change_order_status(uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.reserve_truck(uuid,integer) TO authenticated;
