import { motion } from 'framer-motion';
import { ReactNode } from 'react';
import { useLocation } from 'react-router-dom';

const pageVariants = {
  initial: { opacity: 0, scale: 0.97, y: 16 },
  animate: { opacity: 1, scale: 1, y: 0 },
  exit: { opacity: 0, scale: 0.98, y: -12 },
};

const pageTransition = {
  duration: 0.3,
  ease: [0.22, 1, 0.36, 1] as [number, number, number, number],
};

// Module-level: tracks the last pathname that triggered the entrance animation.
// Persists across remounts so a tab switch (same pathname, component remounts)
// does not replay the animation.
let lastAnimatedPath: string | null = null;

export function PageTransition({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  const shouldAnimate = lastAnimatedPath !== pathname;
  if (shouldAnimate) lastAnimatedPath = pathname;

  return (
    <motion.div
      variants={pageVariants}
      initial={shouldAnimate ? 'initial' : false}
      animate="animate"
      exit="exit"
      transition={pageTransition}
    >
      {children}
    </motion.div>
  );
}