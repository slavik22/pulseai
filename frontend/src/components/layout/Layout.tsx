import { type ReactNode } from 'react'
import { Sidebar } from './Sidebar'

interface Props {
  children: ReactNode
}

export function Layout({ children }: Props) {
  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar />
      <main className="flex-1 ml-64 min-h-screen">
        {children}
      </main>
    </div>
  )
}
