const normalise = (name: string) => name.trim().toLowerCase();

/** Names in {@code selected} that clash with an existing name, or with each other, ignoring case. */
export function duplicateNames(selected: string[], existing: string[]): string[] {
  const taken = new Set(existing.map(normalise));
  const clashes: string[] = [];
  for (const name of selected) {
    const key = normalise(name);
    if (taken.has(key)) clashes.push(name);
    taken.add(key);
  }
  return clashes;
}
