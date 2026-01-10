/**
 * Steam ID utility functions
 */

/**
 * Extract numeric part from Steam ID
 * e.g., "[U:1:123456789]" -> "123456789"
 * 
 * Also handles cases where:
 * - ID is already just a number (returns as-is)
 * - ID is in an unknown format (returns original)
 * 
 * @param fullId The full Steam ID string (e.g., "[U:1:123456789]")
 * @returns The numeric part of the Steam ID, or the original string if no match
 */
export const extractSteamId = (fullId: string | undefined | null): string => {
  if (!fullId) return '';
  // If it's in [U:X:NUMBER] format, extract the number
  const match = fullId.match(/\[U:\d+:(\d+)\]/);
  if (match) return match[1];
  // If it's already just a number, return as-is
  if (/^\d+$/.test(fullId)) return fullId;
  // Otherwise return the original (fallback)
  return fullId;
};
