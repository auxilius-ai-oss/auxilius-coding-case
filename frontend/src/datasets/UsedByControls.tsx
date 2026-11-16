import { Box, Link, Typography } from '@mui/material';
import type { ControlRef } from '../api/types';

export function UsedByControls({ controls, action }: { controls: ControlRef[]; action: string }) {
  return (
    <Box>
      <Typography variant="body2">
        Can't {action}: this version is used by {controls.length} control{controls.length === 1 ? '' : 's'}.
      </Typography>
      <Box component="ul" sx={{ m: 0, pl: 2 }}>
        {controls.map((c) => (
          <li key={c.id}>
            <Link color="inherit" href={`#control-${c.id}`} underline="always">
              {c.name}
            </Link>
          </li>
        ))}
      </Box>
    </Box>
  );
}
