const colors = {
  // Osnovne boje
  primary: '#99F76F',
  background: '#2F2C34',
  surface: '#4A4452',
  border: '#D9D9D9',

  // Tekst i ikone
  textPrimary: '#FFFFFF',
  textLight: '#F2F2F2',
  textSecondary: '#B5B5B5',
  textMuted: '#868686',
  textWorkoutDetails: '#A88686',
  iconMuted: '#C1BCBC',
  inputPlaceholder: '#B3B3B3',

  error: '#FF6B6B',

  // Dodatne boje prisutne na ekranima
  surfaceTeal: '#182526',
  surfaceSlate: '#434B53',
  surfaceBrown: '#554949',
  accentRed: '#8A3333',
  accentPurple: '#7C57FF',
  accentPurpleLight: '#B3A0FF',

  black: '#000000',
  gray950: '#1E1E1E',
  gray900: '#222222',
  gray850: '#232323',
  gray800: '#2C2C2C',
  gray600: '#686868',
  gray500: '#777777',
  gray400: '#A1A1A1',
  gray200: '#E6E5E6',
  gray100: '#F5F5F5',
  navyDark: '#111827',

  // Boje sa transparentnoscu
  overlayBrown: 'rgba(51, 47, 44, 0.5)',
  shadow: 'rgba(0, 0, 0, 0.25)',
  white96: 'rgba(255, 255, 255, 0.96)',
  white88: 'rgba(255, 255, 255, 0.88)',
  overlay: 'rgba(0, 0, 0, 0.6)', // zatamnjenje iza dijaloga
};


const fonts = {
  inter400: 'Inter_400Regular',
  inter500: 'Inter_500Medium',
  inter600: 'Inter_600SemiBold',
  montserrat500: 'Montserrat_500Medium',
  montserrat600: 'Montserrat_600SemiBold',
  spartan300: 'LeagueSpartan_300Light',
  spartan400: 'LeagueSpartan_400Regular',
  spartan500: 'LeagueSpartan_500Medium',
  spartan600: 'LeagueSpartan_600SemiBold',
  roboto400: 'Roboto_400Regular',
  actor400: 'Actor_400Regular',
};

export const theme = {
  colors,
  fonts,

  // Gotovi stilovi teksta. Vrijednosti uskladiti sa Figmom (Inspect panel).
  typography: {
    screenTitle: { fontFamily: fonts.montserrat600, fontSize: 24, color: colors.textPrimary },
    sectionTitle: { fontFamily: fonts.montserrat600, fontSize: 20, color: colors.textLight },
    body: { fontFamily: fonts.inter400, fontSize: 16, color: colors.textPrimary },
    label: { fontFamily: fonts.actor400, fontSize: 18, color: colors.border },
    caption: { fontFamily: fonts.inter400, fontSize: 13, color: colors.textMuted },
    error: { fontFamily: fonts.inter400, fontSize: 14, color: colors.error },
  },

  spacing: {
    none: 0,
    px2: 2,
    px4: 4,
    px5: 5,
    px6: 6,
    px7: 7,
    px8: 8,
    px10: 10,
    px12: 12,
    px14: 14,
    px16: 16,
    px24: 24,
    px26: 26,
    px32: 32,
    px34: 34,

    // Nestandardne vrijednosti na filterima
    filterPaddingBottom: 3.6724138259887695,
    filterPaddingTop: 4.896551609039307,
    filterGap: 4.896551609039307,
    filterPaddingHorizontal: 7.344827651977539,
  },

  borderRadius: {
    none: 0,
    px1: 1,
    px2: 2,
    px4: 4,
    px6: 6,
    px8: 8,
    px10: 10,
    px12: 12,
    px14: 14,
    px15: 15,
    px16: 16,
    px20: 20,
    px22: 22,
    px29: 29,
    px30: 30,
    px58: 58,

    // Nestandardne vrijednosti
    filter: 23.25861930847168,
    frame17: 96.96261596679688,
  },
};

export default theme;
