import { Button, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from '@mui/material';
import { useEffect, useState } from 'react';

interface Props {
  open: boolean;
  mode: 'create' | 'rename';
  initialName: string;
  saving: boolean;
  onClose: () => void;
  onSubmit: (name: string) => void;
}

export function VersionNameDialog({ open, mode, initialName, saving, onClose, onSubmit }: Props) {
  const [name, setName] = useState(initialName);

  useEffect(() => {
    if (open) setName(initialName);
  }, [open, initialName]);

  const submit = () => {
    if (name.trim() && !saving) onSubmit(name.trim());
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{mode === 'create' ? 'New version' : 'Rename version'}</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          fullWidth
          margin="dense"
          label="Version name"
          placeholder="e.g. Q1 2025"
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') submit();
          }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          onClick={submit}
          disabled={!name.trim() || saving}
          startIcon={saving ? <CircularProgress size={16} /> : undefined}
        >
          {mode === 'create' ? 'Create' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
