import { describe, expect, it } from 'vitest';
import { duplicateNames } from './duplicates';

describe('duplicateNames', () => {
  it('ignores case and surrounding spaces', () => {
    expect(duplicateNames(['Vendors.CSV '], ['vendors.csv'])).toEqual(['Vendors.CSV ']);
  });

  it('catches clashes inside the selection', () => {
    expect(duplicateNames(['a.csv', 'A.csv'], [])).toEqual(['A.csv']);
  });

  it('passes unique names', () => {
    expect(duplicateNames(['a.csv', 'b.csv'], ['c.csv'])).toEqual([]);
  });
});
