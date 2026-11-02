"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ArrowUpRight, Leaf, Menu, X } from "lucide-react";
import { getSupabase, isConfigured } from "@/lib/supabase";

export function SiteHeader() {
  const [signedIn, setSignedIn] = useState(false);
  const [menu, setMenu] = useState(false);

  useEffect(() => {
    if (!isConfigured()) return;
    const client = getSupabase();
    void client.auth.getUser().then(({ data }) => setSignedIn(Boolean(data.user)));
    const { data: { subscription } } = client.auth.onAuthStateChange((_event, session) => {
      setSignedIn(Boolean(session?.user));
    });
    return () => subscription.unsubscribe();
  }, []);

  return (
    <header className="site-header">
      <div className="container nav-inner">
        <Link className="brand" href="/" onClick={() => setMenu(false)}>
          <span className="brand-mark"><Leaf size={23} strokeWidth={2.5} /></span>
          <span>agrolink<span className="brand-bd">bd</span><span className="brand-dot">.</span></span>
        </Link>
        <nav className={"nav-links " + (menu ? "nav-open" : "")} aria-label="Main navigation">
          <Link href="/#marketplace" onClick={() => setMenu(false)}>Marketplace</Link>
          <Link href="/#how-it-works" onClick={() => setMenu(false)}>How it works</Link>
          <Link href="/#transport" onClick={() => setMenu(false)}>Transport</Link>
          <Link href={signedIn ? "/dashboard" : "/auth"} className="button button-small button-dark"
            onClick={() => setMenu(false)}>
            {signedIn ? "My dashboard" : "Get started"} <ArrowUpRight size={16} />
          </Link>
        </nav>
        <button type="button" className="mobile-menu" onClick={() => setMenu(!menu)}
          aria-label={menu ? "Close menu" : "Open menu"} aria-expanded={menu}>
          {menu ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>
    </header>
  );
}
