export type Role = "farmer" | "buyer" | "transporter";

export type Profile = {
  id: string;
  full_name: string;
  district: string;
  role: Role;
};

export type Listing = {
  id: string;
  farmer_id: string;
  crop_name: string;
  category: string;
  district: string;
  description: string | null;
  price_per_kg: number;
  stock_kg: number;
  created_at: string;
  status: "active" | "paused";
  profiles?: { full_name: string; district: string } | null;
};

export type Truck = {
  id: string;
  owner_id: string;
  origin: string;
  destination: string;
  capacity_kg: number;
  reserved_kg: number;
  price_per_kg: number;
  departure_date: string;
  status: "active" | "closed";
};

export type Order = {
  id: string;
  listing_id: string;
  buyer_id: string;
  quantity_kg: number;
  total_bdt: number;
  status: "pending" | "confirmed" | "dispatched" | "cancelled";
  created_at: string;
  listings?: { crop_name: string; farmer_id: string } | null;
};

export type OrderMessage = {
  id: string;
  order_id: string;
  sender_id: string;
  body: string;
  created_at: string;
};

export type TruckBooking = {
  id: string;
  truck_id: string;
  user_id: string;
  weight_kg: number;
  total_bdt: number;
  created_at: string;
  trucks?: { origin: string; destination: string; owner_id: string } | null;
};

export const DISTRICTS = [
  "Dhaka", "Gazipur", "Narayanganj", "Mymensingh", "Tangail",
  "Chattogram", "Cumilla", "Feni", "Cox's Bazar", "Sylhet",
  "Rajshahi", "Bogura", "Pabna", "Rangpur", "Dinajpur",
  "Khulna", "Jashore", "Satkhira", "Barishal", "Faridpur",
  "Kushtia", "Jamalpur", "Noakhali", "Narsingdi", "Other"
] as const;

export const CATEGORIES = ["Vegetables", "Fruits", "Rice & Grains", "Spices", "Other"] as const;

export function taka(value: number | string | null | undefined): string {
  const number = Number(value ?? 0);
  return "৳" + (Number.isFinite(number) ? number.toLocaleString("en-BD", {
    maximumFractionDigits: 2
  }) : "0");
}

export function cropEmoji(category: string): string {
  switch (category) {
    case "Vegetables": return "🥬";
    case "Fruits": return "🥭";
    case "Rice & Grains": return "🌾";
    case "Spices": return "🌶️";
    default: return "🌱";
  }
}
