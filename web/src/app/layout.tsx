import type { Metadata } from "next";
import "./globals.css";
import { SiteHeader } from "@/components/site-header";

export const metadata: Metadata = {
  title: "AgroLink BD | From Farm to Market",
  description:
    "A modern Bangladesh-focused agricultural marketplace connecting farmers, buyers and shared transport.",
  applicationName: "AgroLink BD",
  robots: { index: true, follow: true },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <SiteHeader />
        <main>{children}</main>
        <footer className="site-footer">
          <div className="container footer-inner">
            <div><strong className="footer-brand">✳ AgroLink BD</strong><p>From the field to the future.</p></div>
            <p>Built for Bangladesh. A student-built MVP — no payments or delivery guarantees.</p>
            <span>© {new Date().getFullYear()} AgroLink BD</span>
          </div>
        </footer>
      </body>
    </html>
  );
}
