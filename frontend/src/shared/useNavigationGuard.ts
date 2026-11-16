import { useEffect } from 'react';
import { useBlocker } from 'react-router';
import { useConfirm, type ConfirmOptions } from './ConfirmProvider';

/** While {@code when} is true, asks before leaving the page, in the app or in the browser. */
export function useNavigationGuard(when: boolean, options: ConfirmOptions, onLeave: () => void) {
  const confirm = useConfirm();
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) => when &&
      (currentLocation.pathname !== nextLocation.pathname || currentLocation.search !== nextLocation.search),
  );

  useEffect(() => {
    if (blocker.state !== 'blocked') return;
    confirm(options).then((leave) => {
      if (leave) {
        onLeave();
        blocker.proceed();
      } else {
        blocker.reset();
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [blocker.state]);

  useEffect(() => {
    if (!when) return;
    const handler = (event: BeforeUnloadEvent) => {
      event.preventDefault();
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [when]);
}
