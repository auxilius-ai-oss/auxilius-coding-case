import type { SchemaFinding } from '../api/types';

export function findingMessage(f: SchemaFinding): { message: string; fix: string } {
  const column = f.columnName ? `"${f.columnName}"` : `Column ${(f.columnIndex ?? 0) + 1}`;
  switch (f.code) {
    case 'BLANK_COLUMN_NAME':
      return {
        message: `Column ${(f.columnIndex ?? 0) + 1} has no name.`,
        fix: 'Give every column a header in the file and upload it again.',
      };
    case 'DUPLICATE_COLUMN_NAME':
      return {
        message: `${column} is used for more than one column.`,
        fix: 'Rename one of the columns in the file and upload it again.',
      };
    case 'INVALID_COLUMN_NAME_CHARACTERS':
      return {
        message: `${column} contains characters other than letters, digits and underscores.`,
        fix: 'We replaced them with underscores.',
      };
    case 'COLUMN_NAME_STARTS_WITH_DIGIT':
      return { message: `${column} starts with a digit.`, fix: 'We added "c_" to the start of the name.' };
    case 'RESERVED_COLUMN_NAME':
      return { message: `${column} is a reserved word.`, fix: 'We added an underscore to the end of the name.' };
    case 'COLUMN_NAME_TOO_LONG':
      return {
        message: `${column} is longer than 63 characters.`,
        fix: 'Shorten it in the file and upload it again.',
      };
    case 'TABLE_NAME_TOO_LONG':
      return {
        message: 'The table name made from the file name is longer than 63 characters.',
        fix: 'We shortened it to 63 characters.',
      };
    case 'MERGED_CELLS':
      return {
        message: `Cells ${f.cellRange} are merged.`,
        fix: 'Only the first cell of a merged range keeps its value.',
      };
  }
}
