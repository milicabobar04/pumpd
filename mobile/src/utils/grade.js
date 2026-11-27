// Prosječna ocjena trenera: server šalje broj zaokružen na jednu decimalu ili null.
export function formatGrade(grade) {
  return grade == null ? 'Nema ocjena' : Number(grade).toFixed(1);
}
