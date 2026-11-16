import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ErrorIcon from '@mui/icons-material/Error';
import WarningIcon from '@mui/icons-material/Warning';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Typography,
} from '@mui/material';
import type { DataFile, SchemaFinding } from '../api/types';
import { findingMessage } from './findingMessages';

function FindingList({ findings }: { findings: SchemaFinding[] }) {
  return (
    <List dense disablePadding>
      {findings.map((f, i) => {
        const { message, fix } = findingMessage(f);
        return (
          <ListItem key={i} alignItems="flex-start" disableGutters>
            <ListItemIcon sx={{ minWidth: 32, mt: 0.5 }}>
              {f.severity === 'ERROR' ? (
                <ErrorIcon color="error" fontSize="small" />
              ) : (
                <WarningIcon color="warning" fontSize="small" />
              )}
            </ListItemIcon>
            <ListItemText primary={message} secondary={fix} />
          </ListItem>
        );
      })}
    </List>
  );
}

export function FindingsDialog({ file, onClose }: { file: DataFile | null; onClose: () => void }) {
  const sheets = file?.schemaFindings ?? [];
  const all = sheets.flat();
  const errors = all.filter((f) => f.severity === 'ERROR').length;
  const warnings = all.length - errors;
  const named = sheets.filter((s) => s.length > 0 && s[0].sheetName !== null);
  const expandOn = errors > 0 ? 'ERROR' : 'WARNING';

  return (
    <Dialog open={file !== null} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        What we found in {file?.fileName}
        <Typography variant="body2" color="text.secondary">
          {errors} error{errors === 1 ? '' : 's'} · {warnings} warning{warnings === 1 ? '' : 's'}
        </Typography>
      </DialogTitle>
      <DialogContent dividers>
        {named.length === 0 ? (
          <FindingList findings={all} />
        ) : (
          named.map((sheet) => (
            <Accordion
              key={sheet[0].sheetName}
              defaultExpanded={sheet.some((f) => f.severity === expandOn)}
              disableGutters
            >
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography>
                  {sheet[0].sheetName} ({sheet.length})
                </Typography>
              </AccordionSummary>
              <AccordionDetails>
                <FindingList findings={sheet} />
              </AccordionDetails>
            </Accordion>
          ))
        )}
      </DialogContent>
      <DialogActions>
        <Button variant="contained" onClick={onClose}>
          Done
        </Button>
      </DialogActions>
    </Dialog>
  );
}
