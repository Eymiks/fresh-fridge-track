import { motion } from 'framer-motion';
import { ReactNode } from 'react';
import { useLocation } from 'react-router-dom';
import { useAppearance } from '@/contexts/AppearanceContext';

const pageVariants = {
  initial: { opacity: 0, y: 10 },
  animate: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.22, ease: [0.25, 0.46, 0.45, 0.94] as [number, number, number, number] },
  },
  exit: {
    opacity: 0,
    transition: { duration: 0.1, ease: 'easeIn' },
  },
};

// Module-level: tracks the last pathname that triggered the entrance animation.
// Persists across remounts so a tab switch (same pathname, component remounts)
// does not replay the animation.
let lastAnimatedPath: string | null = null;

export function PageTransition({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  const { reduceMotion } = useAppearance();
  const shouldAnimate = !reduceMotion && lastAnimatedPath !== pathname;
  if (!reduceMotion && lastAnimatedPath !== pathname) lastAnimatedPath = pathname;

  return (
    <motion.div
      variants={pageVariants}
      initial={shouldAnimate ? 'initial' : false}
      animate="animate"
      exit="exit"
    >
      {children}
    </motion.div>
  );
}