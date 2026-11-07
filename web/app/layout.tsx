import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = { title: "Midiastory | Chat", description: "Assistente pessoal com busca nos seus documentos." };

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="pt-BR"><body>{children}</body></html>;
}
