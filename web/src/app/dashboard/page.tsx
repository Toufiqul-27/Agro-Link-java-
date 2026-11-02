"use client";

import Link from "next/link";
import { type FormEvent, useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ArrowRight, ArrowUpRight, Box, CalendarDays, CheckCircle2,
  ChevronRight, CircleAlert, LayoutDashboard, LogOut, MapPin,
  Package, Plus, RefreshCw, ShoppingBasket, Sprout, Truck as TruckIcon, Wallet
} from "lucide-react";
import { getSupabase, isConfigured } from "@/lib/supabase";
import {
  CATEGORIES, DISTRICTS, cropEmoji, taka,
  type Listing, type Order, type Profile, type Truck, type TruckBooking
} from "@/lib/types";

type Tab = "market" | "listings" | "orders" | "transport";
type Task = { label: string; value: Tab; icon: typeof Sprout };

function StatusPill({ status }: { status: string }) {
  return <span className={"status-pill " + (status === "cancelled" ? "muted" : status === "dispatched" || status === "confirmed" ? "positive" : "")}>{status}</span>;
}

export default function Dashboard() {
  const router = useRouter();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [userId, setUserId] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [tab, setTab] = useState<Tab>("market");
  const [allListings, setAllListings] = useState<Listing[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [trucks, setTrucks] = useState<TruckIcon[]>([]);
  const [bookings, setBookings] = useState<TruckBooking[]>([]);
  const [cropForm, setCropForm] = useState(false);
  const [truckForm, setTruckForm] = useState(false);
  const [quantity, setQuantity] = useState<Record<string, string>>({});
  const [weights, setWeights] = useState<Record<string, string>>({});

  const refresh = useCallback(async () => {
    if (!isConfigured()) return;
    const sb = getSupabase();
    const [crops, existingOrders, routes, reservations] = await Promise.all([
      sb.from("listings").select("*, profiles(full_name,district)").order("created_at", { ascending: false }).limit(100),
      sb.from("orders").select("*, listings(crop_name,farmer_id)").order("created_at", { ascending: false }).limit(100),
      sb.from("trucks").select("*").order("departure_date", { ascending: true }).limit(100),
      sb.from("truck_bookings").select("*, trucks(origin,destination,owner_id)").order("created_at", { ascending: false }).limit(100)
    ]);
    const firstError = [crops.error, existingOrders.error, routes.error, reservations.error].find(Boolean);
    if (firstError) setError("Unable to load data. Confirm that web/supabase/schema.sql has been applied to Supabase.");
    else setError("");
    setAllListings((crops.data ?? []) as unknown as Listing[]);
    setOrders((existingOrders.data ?? []) as unknown as Order[]);
    setTrucks((routes.data ?? []) as unknown as Truck[]);
    setBookings((reservations.data ?? []) as unknown as TruckBooking[]);
  }, []);

  useEffect(() => {
    let cancelled = false;
    async function initialize() {
      if (!isConfigured()) { setError("Supabase is not connected. Follow web/README.md to configure the project."); setLoading(false); return; }
      const client = getSupabase();
      const { data: auth, error: authError } = await client.auth.getUser();
      if (cancelled) return;
      if (authError || !auth.user) { router.replace("/auth"); return; }
      const { data: person, error: profileError } = await client.from("profiles").select("*").eq("id", auth.user.id).single();
      if (cancelled) return;
      if (profileError || !person) { setError("Profile not found. Check your database setup and signup trigger."); setLoading(false); return; }
      const p = person as Profile;
      setUserId(auth.user.id);
      setProfile(p);
      setTab(p.role === "farmer" ? "listings" : p.role === "transporter" ? "transport" : "market");
      await refresh();
      if (!cancelled) setLoading(false);
    }
    void initialize();
    return () => { cancelled = true; };
  }, [router, refresh]);

  async function perform(promise: PromiseLike<{ error: { message: string } | null }>, success: string) {
    setError(""); setNotice(""); setBusy(true);
    try {
      const { error: actionError } = await promise;
      if (actionError) throw new Error(actionError.message);
      setNotice(success);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not complete your request.");
    } finally {
      setBusy(false);
    }
  }

  async function createCrop(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (profile?.role !== "farmer") return;
    const form = event.currentTarget;
    const data = new FormData(form);
    const price = Number(data.get("price_per_kg"));
    const stock = Number(data.get("stock_kg"));
    if (!Number.isFinite(price) || price <= 0 || !Number.isInteger(stock) || stock <= 0) {
      setError("Enter a positive price and a whole-number stock quantity."); return;
    }
    await perform(
      getSupabase().from("listings").insert({
        farmer_id: userId,
        crop_name: String(data.get("crop_name") || "").trim(),
        category: String(data.get("category") || ""),
        district: String(data.get("district") || ""),
        description: String(data.get("description") || "").trim(),
        price_per_kg: price, stock_kg: stock
      }),
      "Your harvest has been listed."
    );
    // Keep the form visible after errors; on success, data reloads into the table.
  }

  async function createTruck(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (profile?.role !== "transporter") return;
    const data = new FormData(event.currentTarget);
    const capacity = Number(data.get("capacity_kg"));
    const rate = Number(data.get("price_per_kg"));
    const departure = String(data.get("departure_date") || "");
    if (!Number.isInteger(capacity) || capacity <= 0 || !Number.isFinite(rate) || rate <= 0) {
      setError("Capacity must be a positive whole number and price must be positive."); return;
    }
    if (departure < new Date().toISOString().slice(0, 10)) {
      setError("Choose a future departure date."); return;
    }
    await perform(
      getSupabase().from("trucks").insert({
        owner_id: userId,
        origin: String(data.get("origin") || "").trim(),
        destination: String(data.get("destination") || "").trim(),
        departure_date: departure, capacity_kg: capacity, price_per_kg: rate
      }),
      "Transport route published."
    );
  }

  async function buy(item: Listing) {
    if (profile?.role !== "buyer") return;
    const kg = Number(quantity[item.id] || "");
    if (!Number.isInteger(kg) || kg <= 0 || kg > item.stock_kg) {
      setError("Enter a positive whole number within available stock."); return;
    }
    await perform(getSupabase().rpc("place_order", {
      p_listing_id: item.id, p_quantity_kg: kg
    }), "Order placed. You can track it in My orders.");
  }

  async function reserve(truck: Truck) {
    if (profile?.role !== "buyer" && profile?.role !== "farmer") return;
    const kg = Number(weights[truck.id] || "");
    if (!Number.isInteger(kg) || kg <= 0 || kg > truck.capacity_kg - truck.reserved_kg) {
      setError("Enter a positive weight within available truck capacity."); return;
    }
    await perform(getSupabase().rpc("reserve_truck", {
      p_truck_id: truck.id, p_weight_kg: kg
    }), "Truck space reserved. Your booking appears below.");
  }

  async function updateOrder(order: Order, status: "confirmed" | "dispatched" | "cancelled") {
    await perform(getSupabase().rpc("change_order_status", {
      p_order_id: order.id, p_status: status
    }), "Order status updated.");
  }

  async function signOut() {
    await getSupabase().auth.signOut();
    router.push("/");
    router.refresh();
  }

  if (loading) return <div className="dashboard-loading"><RefreshCw size={22} className="spin"/> Loading your AgroLink dashboard…</div>;
  if (!profile) return <div className="container setup-alert"><CircleAlert size={22}/>{error || "Account profile is unavailable."} <Link href="/auth">Sign in</Link></div>;

  const tabs: Task[] = [
    { label: "Marketplace", value: "market", icon: ShoppingBasket },
    ...(profile.role === "farmer" ? [{ label: "My harvests", value: "listings" as Tab, icon: Sprout }] : []),
    ...(profile.role !== "transporter" ? [{ label: "My orders", value: "orders" as Tab, icon: Package }] : []),
    { label: "Transport", value: "transport", icon: TruckIcon }
  ];
  const visibleListings = allListings.filter(item => item.status === "active" && item.stock_kg > 0);
  const myListings = allListings.filter(item => item.farmer_id === userId);
  const myTrucks = trucks.filter(item => item.owner_id === userId);
  const activeTrucks = trucks.filter(item => item.status === "active" && item.capacity_kg > item.reserved_kg && item.departure_date >= new Date().toISOString().slice(0,10));
  const visibleOrders = orders.filter(order =>
    profile.role === "farmer" ? order.listings?.farmer_id === userId : order.buyer_id === userId
  );
  const visibleBookings = bookings.filter(book =>
    profile.role === "transporter" ? book.trucks?.owner_id === userId : book.user_id === userId
  );
  const firstName = profile.full_name.split(" ")[0];

  return (
    <section className="dashboard-shell">
      <div className="container dashboard-head">
        <div><div className="dashboard-welcome">WORKSPACE <ChevronRight size={13}/> {profile.role.toUpperCase()}</div>
          <h1>Good to see you, {firstName}<span className="wave">✳</span></h1>
          <p>Here&apos;s what&apos;s happening across your AgroLink workspace.</p>
        </div>
        <button type="button" onClick={signOut} className="button button-outline dashboard-signout"><LogOut size={17}/> Sign out</button>
      </div>

      <div className="container">
        <div className="dashboard-summary">
          <div><span className="stat-icon green"><LayoutDashboard size={20}/></span><small>Your role</small><strong className="capitalize">{profile.role}</strong></div>
          <div><span className="stat-icon gold"><Box size={20}/></span><small>{profile.role === "farmer" ? "My produce" : profile.role === "transporter" ? "My routes" : "Available produce"}</small><strong>{profile.role === "farmer" ? myListings.length : profile.role === "transporter" ? myTrucks.length : visibleListings.length}</strong></div>
          <div><span className="stat-icon violet"><ShoppingBasket size={20}/></span><small>{profile.role === "transporter" ? "Truck reservations" : "Orders"}</small><strong>{profile.role === "transporter" ? visibleBookings.length : visibleOrders.length}</strong></div>
          <div><span className="stat-icon blue"><TruckIcon size={20}/></span><small>Open truck routes</small><strong>{activeTrucks.length}</strong></div>
        </div>
        <div className="dashboard-layout">
          <aside className="dashboard-sidebar" aria-label="Dashboard sections">
            <span className="sidebar-label">WORKSPACE MENU</span>
            {tabs.map(link => <button type="button" key={link.value}
              className={"sidebar-item "+(tab===link.value?"active":"")}
              onClick={()=>{setTab(link.value);setError("");setNotice("");}}>
              <link.icon size={18}/>{link.label}<ChevronRight size={14} className="sidebar-chevron"/>
            </button>)}
            <div className="sidebar-help"><Sprout size={22}/><strong>Growing together.</strong><p>Start small. Build a stronger local supply chain.</p></div>
          </aside>
          <div className="dashboard-content">
            {error && <div className="form-message" role="alert"><CircleAlert size={18}/>{error}</div>}
            {notice && <div className="form-message good" role="status"><CheckCircle2 size={18}/>{notice}</div>}

            {tab === "market" && <>
              <div className="content-heading"><div><span className="section-kicker">DISCOVER</span><h2>Crop marketplace</h2><p>Browse available farmer produce with transparent pricing.</p></div><button className="icon-action" onClick={()=>void refresh()} aria-label="Refresh marketplace"><RefreshCw size={18}/></button></div>
              <div className="dashboard-cards">
                {visibleListings.length === 0 ? <div className="small-empty">No active crop listings yet. Farmers can publish the first harvest.</div> :
                  visibleListings.map(item=><article className="mini-product-card" key={item.id}>
                    <div className="mini-product-top"><span className="mini-crop-icon">{cropEmoji(item.category)}</span><span className="mini-product-tag">{item.category}</span></div>
                    <h3>{item.crop_name}</h3><p><MapPin size={14}/>{item.district} · {item.profiles?.full_name || "Local farmer"}</p>
                    <div className="mini-product-price"><strong>{taka(item.price_per_kg)}<small> /kg</small></strong><span>{item.stock_kg} kg in stock</span></div>
                    {profile.role === "buyer" && <div className="inline-order">
                      <input aria-label={"Quantity for "+item.crop_name} inputMode="numeric" type="number" min="1" max={item.stock_kg} step="1" placeholder="Kg"
                        value={quantity[item.id]||""} onChange={e=>setQuantity(s=>({...s,[item.id]:e.target.value}))}/>
                      <button disabled={busy} type="button" onClick={()=>void buy(item)} className="button button-lime button-small">Order <ArrowRight size={15}/></button>
                    </div>}
                  </article>)}
              </div>
              {profile.role !== "buyer" && <p className="helper-text">Only buyer accounts can place crop orders. All members can browse listings.</p>}
            </>}

            {tab === "listings" && profile.role === "farmer" && <>
              <div className="content-heading"><div><span className="section-kicker">MY FARM</span><h2>Your harvests</h2><p>Make your produce visible to buyers across Bangladesh.</p></div>
                <button className="button button-dark button-small" type="button" onClick={()=>setCropForm(!cropForm)}><Plus size={17}/>{cropForm?"Hide form":"Add harvest"}</button></div>
              {cropForm && <form className="dashboard-form" onSubmit={createCrop}>
                <div className="form-grid">
                  <label>Crop name<input name="crop_name" required minLength={2} maxLength={100} placeholder="e.g. Fresh potatoes"/></label>
                  <label>Category<select required name="category">{CATEGORIES.map(c=><option key={c}>{c}</option>)}</select></label>
                  <label>District<select name="district" defaultValue={profile.district}>{DISTRICTS.map(d=><option key={d}>{d}</option>)}</select></label>
                  <label>Price per kg (BDT)<input type="number" name="price_per_kg" required min="0.01" step="0.01" placeholder="৳"/></label>
                  <label>Available quantity (kg)<input type="number" name="stock_kg" required min="1" max="1000000" step="1"/></label>
                  <label className="span-two">Short description<textarea name="description" rows={3} maxLength={1000} placeholder="Harvest details, quality, pickup conditions..."/></label>
                </div>
                <button disabled={busy} className="button button-dark" type="submit">Publish harvest <ArrowUpRight size={16}/></button>
              </form>}
              <div className="list-table">
                <div className="table-head"><span>PRODUCE</span><span>PRICE / KG</span><span>STOCK</span><span>STATUS</span></div>
                {myListings.length===0?<div className="small-empty">No harvests listed yet. Add your first crop above.</div>:
                  myListings.map(item=><div className="table-row" key={item.id}>
                    <span><span className="inline-emoji">{cropEmoji(item.category)}</span><strong>{item.crop_name}</strong><small>{item.district}</small></span>
                    <span>{taka(item.price_per_kg)}</span><span>{item.stock_kg} kg</span><span><StatusPill status={item.status}/></span>
                  </div>)}
              </div>
            </>}

            {tab === "orders" && profile.role !== "transporter" && <>
              <div className="content-heading"><div><span className="section-kicker">TRADE</span><h2>{profile.role === "buyer"?"My orders":"Incoming orders"}</h2><p>Track orders and act on the next step.</p></div></div>
              <div className="orders-list">{visibleOrders.length===0?<div className="small-empty">No orders yet. {profile.role==="buyer"?"Explore the marketplace to get started.":"Orders for your crops will appear here."}</div>:
                visibleOrders.map(order=><article className="order-item" key={order.id}>
                  <div className="order-icon"><ShoppingBasket size={22}/></div>
                  <div className="order-details"><strong>{order.listings?.crop_name || "Crop order"}</strong><small>{order.quantity_kg} kg · {new Date(order.created_at).toLocaleDateString("en-BD")}</small></div>
                  <div className="order-price"><strong>{taka(order.total_bdt)}</strong><StatusPill status={order.status}/></div>
                  <div className="order-actions">
                    {profile.role === "buyer" && order.status === "pending" &&
                      <button disabled={busy} type="button" onClick={()=>void updateOrder(order,"cancelled")}>Cancel</button>}
                    {profile.role === "farmer" && order.status === "pending" &&
                      <button disabled={busy} type="button" onClick={()=>void updateOrder(order,"confirmed")}>Confirm</button>}
                    {profile.role === "farmer" && order.status === "confirmed" &&
                      <button disabled={busy} type="button" onClick={()=>void updateOrder(order,"dispatched")}>Dispatched</button>}
                  </div>
                </article>)}</div>
            </>}

            {tab === "transport" && <>
              <div className="content-heading"><div><span className="section-kicker">LOGISTICS</span><h2>Shared transport</h2><p>Publish routes or reserve unused truck capacity.</p></div>
                {profile.role === "transporter" && <button type="button" className="button button-dark button-small" onClick={()=>setTruckForm(!truckForm)}><Plus size={17}/>{truckForm?"Hide form":"Add route"}</button>}
              </div>
              {truckForm && profile.role==="transporter" && <form className="dashboard-form" onSubmit={createTruck}>
                <div className="form-grid">
                  <label>Origin<input name="origin" required minLength={2} maxLength={100} placeholder="e.g. Bogura"/></label>
                  <label>Destination<input name="destination" required minLength={2} maxLength={100} placeholder="e.g. Dhaka"/></label>
                  <label>Departure date<input type="date" name="departure_date" min={new Date().toISOString().slice(0,10)} required/></label>
                  <label>Total capacity (kg)<input type="number" name="capacity_kg" min="1" step="1" max="1000000" required/></label>
                  <label>Cost per kg (BDT)<input type="number" name="price_per_kg" min="0.01" step="0.01" required/></label>
                </div><button disabled={busy} type="submit" className="button button-dark">Publish route <ArrowUpRight size={16}/></button>
              </form>}
              <div className="route-grid">
                {(profile.role==="transporter"?myTrucks:activeTrucks).length===0?
                  <div className="small-empty">No truck routes available right now.</div> :
                  (profile.role==="transporter"?myTrucks:activeTrucks).map(truck=><div className="route-card" key={truck.id}>
                    <div className="route-icon"><TruckIcon size={24}/><span><CalendarDays size={13}/>{truck.departure_date}</span></div>
                    <div className="route-locations"><strong>{truck.origin}</strong><ArrowRight size={19}/><strong>{truck.destination}</strong></div>
                    <div className="route-stats"><span><small>AVAILABLE</small><b>{truck.capacity_kg-truck.reserved_kg} kg</b></span><span><small>RATE</small><b>{taka(truck.price_per_kg)} /kg</b></span></div>
                    {profile.role!=="transporter" &&
                      <div className="inline-order"><input type="number" min="1" max={truck.capacity_kg-truck.reserved_kg} step="1" placeholder="Weight (kg)"
                        aria-label={"Weight for "+truck.origin+" to "+truck.destination} value={weights[truck.id]||""}
                        onChange={e=>setWeights(s=>({...s,[truck.id]:e.target.value}))}/>
                        <button disabled={busy} onClick={()=>void reserve(truck)} type="button" className="button button-lime button-small">Reserve <ArrowRight size={15}/></button>
                      </div>}
                  </div>)}
              </div>
              <h3 className="subheading">{profile.role==="transporter"?"Reservations on your routes":"Your truck reservations"}</h3>
              <div className="orders-list">{visibleBookings.length===0?<div className="small-empty">No reservations recorded yet.</div>:
                visibleBookings.map(book=><div className="booking-row" key={book.id}>
                  <span><TruckIcon size={18}/><strong>{book.trucks?.origin} → {book.trucks?.destination}</strong></span>
                  <span>{book.weight_kg} kg</span><strong>{taka(book.total_bdt)}</strong>
                </div>)}
              </div>
            </>}
            <div className="dashboard-note"><CircleAlert size={17}/>
              AgroLink is a pilot marketplace. Confirm crop quality, pickup and payment directly with the counterparty. Reservations are recorded, not a guaranteed delivery service.
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
