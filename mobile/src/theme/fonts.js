import { useFonts } from 'expo-font';

// Svaki font se uvozi iz svog podfoldera (npr. '@expo-google-fonts/inter/400Regular').
// Uvoz iz korijena paketa ('@expo-google-fonts/inter') bi u aplikaciju ubacio
// SVE debljine tog fonta (~16 MB), a koristi se samo ovih 11.
import { Inter_400Regular } from '@expo-google-fonts/inter/400Regular';
import { Inter_500Medium } from '@expo-google-fonts/inter/500Medium';
import { Inter_600SemiBold } from '@expo-google-fonts/inter/600SemiBold';
import { Montserrat_500Medium } from '@expo-google-fonts/montserrat/500Medium';
import { Montserrat_600SemiBold } from '@expo-google-fonts/montserrat/600SemiBold';
import { LeagueSpartan_300Light } from '@expo-google-fonts/league-spartan/300Light';
import { LeagueSpartan_400Regular } from '@expo-google-fonts/league-spartan/400Regular';
import { LeagueSpartan_500Medium } from '@expo-google-fonts/league-spartan/500Medium';
import { LeagueSpartan_600SemiBold } from '@expo-google-fonts/league-spartan/600SemiBold';
import { Roboto_400Regular } from '@expo-google-fonts/roboto/400Regular';
import { Actor_400Regular } from '@expo-google-fonts/actor/400Regular';

// Ključevi ovog objekta postaju nazivi fontova (fontFamily) i moraju biti isti kao u theme.fonts.
export function useAppFonts() {
  const [loaded, error] = useFonts({
    Inter_400Regular, Inter_500Medium, Inter_600SemiBold,
    Montserrat_500Medium, Montserrat_600SemiBold,
    LeagueSpartan_300Light, LeagueSpartan_400Regular,
    LeagueSpartan_500Medium, LeagueSpartan_600SemiBold,
    Roboto_400Regular,
    Actor_400Regular,
  });

  // Ako font ne uspije da se učita, aplikacija ipak radi sa sistemskim fontom.
  return loaded || !!error;
}
