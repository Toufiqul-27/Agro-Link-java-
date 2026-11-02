"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  ArrowRight, ArrowUpRight, Check, CircleHelp, Leaf, MapPin,
  MoveUpRight, Search, ShieldCheck, Sprout, Truck, Wheat
} from "lucide-react";
import { getSupabase, isConfigured } from "@/lib/supabase";
import { CATEGORIES, cropEmoji, taka, type Listing } from "@/lib/types";

const workflow = [
  { step: "01", title: "Farmers post harvests", detail: "Create a produce listing with quantity, district and an asking price.", icon: Sprout },
  { step: "02", title: "Buyers place orders", detail: "Discover available crops and submit an order while stock is still available.", icon: Search },
  { step: "03", title: "Share the journey", detail: "See available truck routes and reserve capacity by the kilogram.", icon: Truck }
];

export default function Home() {
  const [listings, setListings] = useState<Listing[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("All produce");
  const [search, setSearch] = useState("");
  const [dbError, setDbError] = useState("");

  useEffect(() => {
    if (!isConfigured()) {
      setLoading(false);
      return;
    }
    getSupabase().from("listings")
      .select("*, profiles(full_name,district)")
      .eq("status", "active")
      .gt("stock_kg", 0)
      .order("created_at", { ascending: false })
      .limit(24)
      .then(({ data, error }) => {
        if (error) setDbError("Marketplace is temporarily unavailable.");
        else setListings((data ?? []) as unknown as Listing[]);
        setLoading(false);
      });
  }, []);

  const filtered = useMemo(() => listings.filter(item => {
    const matchesCategory = filter === "All produce" || item.category === filter;
    const haystack = (item.crop_name + " " + item.district + " " + item.category).toLowerCase();
    return matchesCategory && haystack.includes(search.trim().toLowerCase());
  }), [listings, filter, search]);

  return (
    <>
      <section className="hero">
        <div className="container hero-grid">
          <div className="hero-copy">
            <div className="eyebrow"><span className="eyebrow-dot" /> Growing a more connected Bangladesh</div>
            <h1>Better harvests.<br /><span>Better connections.</span></h1>
            <p className="hero-intro">
              A smarter way to bring farmers, produce buyers and shared transport
              together — from the village field to the city market.
            </p>
            <div className="hero-actions">
              <Link href="/#marketplace" className="button button-lime">Explore the marketplace <ArrowUpRight size={18} /></Link>
              <Link href="/auth" className="button button-outline">Join AgroLink <ArrowRight size={18} /></Link>
            </div>
            <div className="hero-footnotes"><span><Check size={16} /> Built for local agriculture</span><span><Check size={16} /> Mobile-first experience</span></div>
          </div>

          <div className="hero-art" aria-label="Illustration of farm produce connected to transport and markets" role="img">
            <span className="orbit orbit-one" /><span className="orbit orbit-two" />
            <div className="art-glow" />
            <div className="field-card">
              <div className="field-scene">
                <span className="field-sun" />
                <span className="field-hill hill-back" />
                <span className="field-hill hill-front" />
                <span className="field-row row-one" /><span className="field-row row-two" /><span className="field-row row-three" />
                <div className="field-icon"><Wheat size={74} strokeWidth={1.4} /></div>
              </div>
              <div className="field-meta"><span><span className="tiny-live" /> CONNECTING THE FOOD CHAIN</span><strong>From farm → market</strong></div>
            </div>
            <div className="floating-tag float-top"><span className="mini-icon">🌱</span><span><small>FOR FARMERS</small><strong>List your harvest</strong></span></div>
            <div className="floating-tag float-bottom"><span className="mini-icon">🚚</span><span><small>SHARED TRANSPORT</small><strong>Book by the kilo</strong></span></div>
            <div className="coordinate-label">23°48&apos; N / 90°24&apos; E<br />BANGLADESH / 2026</div>
          </div>
        </div>
        <div className="container"><div className="hero-divider"><span>ONE PLATFORM. MORE POSSIBILITIES.</span><span>SCROLL TO EXPLORE ↓</span></div></div>
      </section>

      <section className="values-strip">
        <div className="container value-grid">
          <div><Leaf size={22}/><span><strong>Farm-first thinking</strong><small>Give local producers more visibility</small></span></div>
          <div><ShieldCheck size={22}/><span><strong>Transparent listings</strong><small>Quantity and prices in BDT</small></span></div>
          <div><Truck size={22}/><span><strong>Shared capacity</strong><small>Reserve truck space without overbooking</small></span></div>
        </div>
      </section>

      <section className="section marketplace-section" id="marketplace">
        <div className="container">
          <div className="section-top">
            <div><span className="section-kicker">THE MARKETPLACE</span><h2>Fresh opportunities,<br/><span>closer to home.</span></h2></div>
            <p>Explore agricultural products posted by local farmers. Discover what is available right now.</p>
          </div>
          <div className="market-toolbar">
            <div className="search-field"><Search size={19}/><input type="search" placeholder="Search crops, districts..." aria-label="Search crops and districts" value={search} onChange={e=>setSearch(e.target.value)} /></div>
            <select className="filter-select" value={filter} onChange={e=>setFilter(e.target.value)} aria-label="Filter crops by category">
              <option>All produce</option>{CATEGORIES.map(cat=><option key={cat}>{cat}</option>)}
            </select>
          </div>
          {loading ? <div className="empty-state">Loading marketplace listings…</div> :
            dbError ? <div className="empty-state">{dbError}</div> :
            filtered.length === 0 ? <div className="empty-state">
              <Sprout size={30} /><h3>{listings.length ? "No matching crops yet" : "The marketplace is just getting started"}</h3>
              <p>{listings.length ? "Try a different search or category." : "Real farmer listings will appear here when members publish them. No fictional inventory."}</p>
              <Link className="button button-dark" href="/auth">Join the marketplace <ArrowUpRight size={16}/></Link>
            </div> :
            <div className="product-grid">{filtered.map(item=><article className="product-card" key={item.id}>
              <div className="product-visual"><span className="product-emoji" aria-hidden>{cropEmoji(item.category)}</span><span className="product-category">{item.category}</span></div>
              <div className="product-content"><span className="product-location"><MapPin size={13}/>{item.district}</span>
                <h3>{item.crop_name}</h3><p className="product-farmer">Listed by {item.profiles?.full_name || "local farmer"}</p>
                <div className="product-bottom"><div><strong>{taka(item.price_per_kg)}</strong><small> / kg</small><p>{item.stock_kg} kg available</p></div>
                  <Link href="/dashboard" className="round-arrow" aria-label={"Order "+item.crop_name}><ArrowUpRight size={20}/></Link>
                </div>
              </div>
            </article>)}</div>
          }
          <p className="section-note"><CircleHelp size={15}/> Listings are member-submitted; inspect quality and agree on fulfilment directly before trading.</p>
        </div>
      </section>

      <section className="dark-section section" id="how-it-works">
        <div className="container">
          <div className="section-top on-dark"><div><span className="section-kicker">HOW IT WORKS</span><h2>Simple steps.<br/><span>Stronger connections.</span></h2></div><p>Designed around real trade workflows, not complicated software.</p></div>
          <div className="steps-grid">{workflow.map(item=><div className="step-card" key={item.step}>
            <span className="step-number">{item.step}</span><div className="step-icon"><item.icon size={30}/></div>
            <h3>{item.title}</h3><p>{item.detail}</p>
            <MoveUpRight size={18} className="step-arrow"/>
          </div>)}</div>
        </div>
      </section>

      <section className="section transport-section" id="transport">
        <div className="container transport-grid">
          <div className="transport-illustration"><div className="road-mark"/><div className="truck-symbol"><Truck size={120} strokeWidth={1.05}/></div><span className="route-point point-a">FARM</span><span className="route-point point-b">MARKET</span><span className="road-dash"/></div>
          <div className="transport-copy"><span className="section-kicker">SMARTER LOGISTICS</span><h2>Don&apos;t pay for<br/><span>empty space.</span></h2>
            <p>Transport providers can publish available routes and capacity. Farmers and buyers can reserve the kilos they need, with transparent estimated pricing.</p>
            <div className="transport-bullets"><span><Check size={17}/> Reserve capacity in kilograms</span><span><Check size={17}/> Track available space before booking</span><span><Check size={17}/> No invented delivery guarantees</span></div>
            <Link href="/dashboard" className="button button-dark">Explore truck routes <ArrowUpRight size={18}/></Link>
          </div>
        </div>
      </section>

      <section className="cta-section"><div className="container">
        <div className="cta-card"><div><span className="section-kicker">LET'S GROW TOGETHER</span><h2>A better food chain<br/>starts with connection.</h2><p>Create an account to list produce, find crops or publish transport capacity.</p></div>
        <Link href="/auth" className="button button-dark">Get started free <ArrowUpRight size={18}/></Link>
        <span className="cta-decoration"><Wheat size={190} strokeWidth={0.8}/></span></div>
      </div></section>
    </>
  );
}
