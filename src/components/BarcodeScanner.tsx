import { useEffect, useRef, useState, useCallback } from 'react';
import { Html5Qrcode } from 'html5-qrcode';
import { X, ScanBarcode } from 'lucide-react';

interface BarcodeScannerProps {
  onScan: (barcode: string) => void;
  onClose: () => void;
}

export function BarcodeScanner({ onScan, onClose }: BarcodeScannerProps) {
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const isRunningRef = useRef(false);
  const hasScannedRef = useRef(false);
  const onScanRef = useRef(onScan);
  const onCloseRef = useRef(onClose);
  const [error, setError] = useState<string | null>(null);

  // Keep latest callbacks without restarting the scanner
  useEffect(() => {
    onScanRef.current = onScan;
    onCloseRef.current = onClose;
  });

  const stopScanner = useCallback(async () => {
    if (scannerRef.current && isRunningRef.current) {
      isRunningRef.current = false;
      try {
        await scannerRef.current.stop();
      } catch {
        // already stopped
      }
    }
  }, []);

  useEffect(() => {
    const scanner = new Html5Qrcode('barcode-reader');
    scannerRef.current = scanner;
    hasScannedRef.current = false;

    scanner
      .start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 280, height: 160 } },
        (decodedText) => {
          if (hasScannedRef.current) return;
          hasScannedRef.current = true;
          stopScanner().then(() => onScanRef.current(decodedText));
        },
        () => {}
      )
      .then(() => {
        isRunningRef.current = true;
      })
      .catch(() => {
        setError("Impossible d'accéder à la caméra. Vérifiez les permissions.");
      });

    return () => {
      stopScanner();
    };
  }, [stopScanner]);

  const handleClose = () => {
    stopScanner().then(() => onCloseRef.current());
  };

  return (
    <div className="fixed inset-0 z-50 bg-background flex flex-col">
      <div className="flex items-center justify-between px-4 py-3 bg-primary text-primary-foreground">
        <div className="flex items-center gap-2">
          <ScanBarcode className="w-5 h-5" />
          <span className="font-bold text-base">Scanner le code-barres</span>
        </div>
        <button
          onClick={handleClose}
          className="p-1.5 rounded-full bg-primary-foreground/20 hover:bg-primary-foreground/30 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="flex-1 relative bg-black">
        <div id="barcode-reader" className="w-full h-full [&>video]:!w-full [&>video]:!h-full [&>video]:!object-cover [&_img]:hidden [&>#qr-shaded-region]:!border-none" />
        
        <div className="absolute inset-0 pointer-events-none flex items-center justify-center">
          <div className="w-72 h-40 relative">
            <div className="absolute top-0 left-0 w-8 h-8 border-primary rounded-tl-lg" style={{ borderWidth: '3px', borderRight: 'none', borderBottom: 'none' }} />
            <div className="absolute top-0 right-0 w-8 h-8 border-primary rounded-tr-lg" style={{ borderWidth: '3px', borderLeft: 'none', borderBottom: 'none' }} />
            <div className="absolute bottom-0 left-0 w-8 h-8 border-primary rounded-bl-lg" style={{ borderWidth: '3px', borderRight: 'none', borderTop: 'none' }} />
            <div className="absolute bottom-0 right-0 w-8 h-8 border-primary rounded-br-lg" style={{ borderWidth: '3px', borderLeft: 'none', borderTop: 'none' }} />
            <div className="absolute left-2 right-2 h-0.5 bg-primary/80 animate-pulse top-1/2" />
          </div>
        </div>
      </div>

      <div className="px-4 py-5 bg-background text-center">
        {error ? (
          <p className="text-sm text-destructive">{error}</p>
        ) : (
          <p className="text-sm text-muted-foreground">
            Placez le code-barres dans le cadre pour le scanner automatiquement
          </p>
        )}
      </div>
    </div>
  );
}
