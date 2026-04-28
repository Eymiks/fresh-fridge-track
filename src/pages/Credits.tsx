import { ArrowLeft, Heart, Code2, Palette, Zap, Globe, LeafyGreen } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageTransition } from '@/components/PageTransition';
import { motion } from 'framer-motion';

const techs = [
  { name: 'React', desc: 'Interface utilisateur', icon: Code2 },
  { name: 'Tailwind CSS', desc: 'Design system', icon: Palette },
  { name: 'Framer Motion', desc: 'Animations fluides', icon: Zap },
  { name: 'OpenFoodFacts', desc: 'Données produits', icon: Globe },
];

const Credits = () => {
  const navigate = useNavigate();

  return (
    <PageTransition>
      <div className="min-h-screen bg-background">
        {/* Header */}
        <div className="flex items-center gap-3 px-5 pt-12 pb-6">
          <button
            onClick={() => navigate('/')}
            className="p-2 rounded-full bg-card border border-border hover:bg-muted transition-colors"
          >
            <ArrowLeft className="w-5 h-5 text-foreground" />
          </button>
          <h1 className="text-xl font-extrabold text-foreground">Crédits</h1>
        </div>

        <div className="px-5 space-y-6">
          {/* App info */}
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-center py-8"
          >
            <div className="w-20 h-20 mx-auto mb-4 bg-primary/10 rounded-3xl flex items-center justify-center">
              <LeafyGreen className="w-10 h-10 text-primary" />
            </div>
            <h2 className="text-2xl font-extrabold text-foreground">FreshTrack</h2>
            <p className="text-sm text-muted-foreground mt-1">Version 1.0.0</p>
            <p className="text-sm text-muted-foreground mt-3 max-w-xs mx-auto">
              Suivez vos produits alimentaires, évitez le gaspillage et gardez votre frigo organisé.
            </p>
          </motion.div>

          {/* Technologies */}
          <div>
            <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-3">Technologies</h3>
            <div className="space-y-2">
              {techs.map((tech, i) => (
                <motion.div
                  key={tech.name}
                  initial={{ opacity: 0, x: -16 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.06 }}
                  className="flex items-center gap-3 bg-card border border-border rounded-2xl p-4"
                >
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                    <tech.icon className="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-card-foreground">{tech.name}</p>
                    <p className="text-xs text-muted-foreground">{tech.desc}</p>
                  </div>
                </motion.div>
              ))}
            </div>
          </div>

          {/* Made with love */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="text-center py-8"
          >
            <div className="flex items-center justify-center gap-1.5 text-sm text-muted-foreground">
              Fait avec <Heart className="w-4 h-4 text-destructive fill-destructive" />
            </div>
          </motion.div>
        </div>
      </div>
    </PageTransition>
  );
};

export default Credits;
