"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, CheckCircle2, Leaf, LockKeyhole, Mail } from "lucide-react";
import { getSupabase, isConfigured } from "@/lib/supabase";
import { DISTRICTS, type Role } from "@/lib/types";

export default function AuthPage() {
  const router = useRouter();
  const [mode, setMode] = useState<"login" | "register">("register");
  const [role, setRole] = useState<Role>("farmer");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!isConfigured()) return;
    void getSupabase().auth.getUser().then(({ data }) => {
      if (data.user) router.replace("/dashboard");
    });
  }, [router]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(""); setBusy(true); setSuccess(false);
    try {
      if (!isConfigured()) throw new Error("Supabase setup is incomplete. Add environment variables first.");
      const data = new FormData(event.currentTarget);
      const email = String(data.get("email") || "").trim();
      const password = String(data.get("password") || "");
      if (password.length < 8) throw new Error("Use at least 8 characters for your password.");
      const client = getSupabase();
      if (mode === "register") {
        const fullName = String(data.get("full_name") || "").trim();
        const district = String(data.get("district") || "").trim();
        if (fullName.length < 2 || !district) throw new Error("Enter your name and district.");
        const { data: result, error } = await client.auth.signUp({
          email, password,
          options: { data: { full_name: fullName, district, role } }
        });
        if (error) throw error;
        if (result.session) router.push("/dashboard");
        else {
          setSuccess(true);
          setMessage("Account request received. Check your email for a confirmation link, then sign in.");
        }
      } else {
        const { error } = await client.auth.signInWithPassword({ email, password });
        if (error) throw error;
        router.push("/dashboard");
        router.refresh();
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Something went wrong. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="auth-section">
      <div className="container auth-grid">
        <div className="auth-aside">
          <span className="section-kicker">JOIN THE MOVEMENT</span>
          <h1>Good things<br/>grow <em>together.</em></h1>
          <p>Bring your next harvest, order or shared journey onto one simple platform.</p>
          <div className="auth-aside-icon"><Leaf size={168} strokeWidth={0.9}/></div>
          <div className="auth-aside-bottom"><span><CheckCircle2 size={17}/> Designed for Bangladesh</span><span><CheckCircle2 size={17}/> No payment details required</span></div>
        </div>
        <div className="auth-panel">
          <Link className="auth-back" href="/"><ArrowRight size={16} className="flipped"/> Back to home</Link>
          <div className="auth-heading"><span className="auth-avatar"><Leaf size={26}/></span><h2>{mode === "register" ? "Create your account" : "Welcome back"}</h2><p>{mode === "register" ? "Start with a few details about yourself." : "Sign in to access your AgroLink dashboard."}</p></div>
          <div className="auth-switch" role="tablist" aria-label="Authentication">
            <button type="button" role="tab" aria-selected={mode==="register"} className={mode==="register"?"selected":""} onClick={()=>{setMode("register");setMessage("");}}>Create account</button>
            <button type="button" role="tab" aria-selected={mode==="login"} className={mode==="login"?"selected":""} onClick={()=>{setMode("login");setMessage("");}}>Sign in</button>
          </div>
          <form className="auth-form" onSubmit={submit}>
            {mode === "register" && <>
              <label>What brings you here?</label>
              <div className="role-choices" role="group" aria-label="Choose your role">
                {([{role:"farmer",icon:"🌾",label:"Farmer"}, {role:"buyer",icon:"🛒",label:"Buyer"}, {role:"transporter",icon:"🚚",label:"Transport"}] as const).map(option=>
                  <button type="button" key={option.role} aria-pressed={role===option.role}
                    onClick={()=>setRole(option.role)} className={role===option.role?"role-choice active":"role-choice"}>
                    <span>{option.icon}</span><small>{option.label}</small>
                  </button>)}
              </div>
              <label htmlFor="full-name">Full name</label>
              <input required minLength={2} maxLength={100} id="full-name" name="full_name" placeholder="Your name" autoComplete="name" />
              <label htmlFor="district">District</label>
              <select required id="district" name="district" defaultValue="">
                <option value="" disabled>Choose your district</option>
                {DISTRICTS.map(d=><option key={d} value={d}>{d}</option>)}
              </select>
            </>}
            <label htmlFor="email">Email address</label><div className="input-icon"><Mail size={18}/><input id="email" name="email" type="email" autoComplete="email" required placeholder="you@example.com"/></div>
            <label htmlFor="password">Password</label><div className="input-icon"><LockKeyhole size={18}/><input id="password" name="password" type="password" minLength={8} autoComplete={mode==="register"?"new-password":"current-password"} required placeholder="At least 8 characters"/></div>
            {message && <div className={success?"form-message good":"form-message"} role="status">{message}</div>}
            <button className="button button-dark auth-submit" disabled={busy}>{busy?"Please wait…":mode==="register"?"Create my account":"Sign in"}<ArrowRight size={18}/></button>
          </form>
          <p className="auth-disclaimer">AgroLink BD is an early-stage MVP. Accounts use Supabase Auth; no payments are processed on this site.</p>
        </div>
      </div>
    </section>
  );
}
