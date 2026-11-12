import * as React from "react"
import { cva } from "class-variance-authority"
import { cn } from "cn"

function Empty({ className, ...props }) {
  return (
    <div
      data-slot="empty"
      className={cn(
        "flex min-h-[340px] w-full flex-col items-center justify-center rounded-2xl border border-border bg-card p-8 text-center text-card-foreground shadow-2xs",
        className
      )}
      {...props}
    />
  )
}

function EmptyHeader({ className, ...props }) {
  return (
    <div
      data-slot="empty-header"
      className={cn("flex max-w-md flex-col items-center gap-2", className)}
      {...props}
    />
  )
}

const emptyMediaVariants = cva(
  "flex items-center justify-center text-muted-foreground",
  {
    variants: {
      variant: {
        default: "",
        icon: "size-12 rounded-xl bg-muted/60 text-muted-foreground [&_svg]:size-6",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

function EmptyMedia({ className, variant = "default", ...props }) {
  return (
    <div
      data-slot="empty-media"
      className={cn(emptyMediaVariants({ variant }), className)}
      {...props}
    />
  )
}

function EmptyTitle({ className, ...props }) {
  return (
    <h3
      data-slot="empty-title"
      className={cn("text-base font-semibold tracking-tight text-foreground", className)}
      {...props}
    />
  )
}

function EmptyDescription({ className, ...props }) {
  return (
    <p
      data-slot="empty-description"
      className={cn("text-xs sm:text-sm text-muted-foreground leading-relaxed", className)}
      {...props}
    />
  )
}

function EmptyContent({ className, ...props }) {
  return (
    <div
      data-slot="empty-content"
      className={cn("mt-6 flex flex-wrap items-center justify-center gap-3", className)}
      {...props}
    />
  )
}

export {
  Empty,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
  EmptyDescription,
  EmptyContent,
}
