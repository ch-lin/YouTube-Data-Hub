"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";


export default function Navbar() {

  const pathname = usePathname();

  const navItems = [
    { href: "/videos", label: "Videos" },
    { href: "/channels", label: "Channels" },
    { href: "/tags", label: "Tags" },
    { href: "/tools", label: "Tools" },
    { href: "/configs", label: "Configs" },
  ];

  return (
    <nav className="bg-white dark:bg-gray-900 shadow-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <Link href="/" className="text-3xl font-bold text-gray-800 dark:text-white">
                YouTube Hub
              </Link>
            </div>
            <div className="hidden md:block">
              <div className="ml-10 flex items-baseline space-x-4">
                {/* <Link href="/channels" className="text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-white px-3 py-2 rounded-md text-sm font-medium ">
                  Channel
                </Link>
                <Link href="/" className="text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-white px-3 py-2 rounded-md text-sm font-medium">
                  Video
                </Link> */}
                {navItems.map((item) => (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`transition-colors ${
                      pathname === item.href ? "text-blue-600" : "text-foreground/60 hover:text-blue-600"
                    }`}>
                    {item.label}
                  </Link>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
}
