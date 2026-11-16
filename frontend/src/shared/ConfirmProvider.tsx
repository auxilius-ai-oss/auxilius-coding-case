import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';
import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react';

export interface ConfirmOptions {
  title: string;
  description: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string | null;
  destructive?: boolean;
}

type Confirm = (options: ConfirmOptions) => Promise<boolean>;

const ConfirmContext = createContext<Confirm | null>(null);

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [options, setOptions] = useState<ConfirmOptions | null>(null);
  const resolver = useRef<(value: boolean) => void>(undefined);

  const confirm = useCallback<Confirm>((next) => {
    setOptions(next);
    return new Promise<boolean>((resolve) => {
      resolver.current = resolve;
    });
  }, []);

  const close = (value: boolean) => {
    resolver.current?.(value);
    setOptions(null);
  };

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <Dialog open={options !== null} onClose={() => close(false)} maxWidth="xs" fullWidth>
        {options && (
          <>
            <DialogTitle>{options.title}</DialogTitle>
            <DialogContent>
              <DialogContentText component="div">{options.description}</DialogContentText>
            </DialogContent>
            <DialogActions>
              {options.cancelLabel !== null && (
                <Button onClick={() => close(false)}>{options.cancelLabel ?? 'Cancel'}</Button>
              )}
              <Button
                variant="contained"
                color={options.destructive ? 'error' : 'primary'}
                onClick={() => close(true)}
                autoFocus
              >
                {options.confirmLabel ?? 'Confirm'}
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>
    </ConfirmContext.Provider>
  );
}

export function useConfirm(): Confirm {
  const confirm = useContext(ConfirmContext);
  if (!confirm) throw new Error('useConfirm needs a ConfirmProvider');
  return confirm;
}
