import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  icons: { icon: "/sentinela-logo.png", apple: "/sentinela-logo.png" },
  title: "Sentinela — Segurança em cada link",
  description:
    "Analise links, entenda os riscos e navegue com mais informação.",
};
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}
