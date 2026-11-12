import { theme } from './theme';

const { colors, fonts, spacing, borderRadius } = theme;

// ---------------------------------------------------------------------
// Mjere iz Figme (jedini izvor za veličine u ovom fajlu)
// ---------------------------------------------------------------------

export const buttonSizes = {
  primary: { width: 111, height: 52 },
  information: { width: 111, height: 52 },
  login: { width: 272, height: 42 },
  action: { width: 322, height: 45 },
  addGroup: { width: 346, height: 66 },
  add: { width: 243, height: 56 },
  chip: { width: 71, height: 17.137929916381836 },
  contentChip: { width: 104, height: 17 },
  textLink: { height: 25.2 },
  dialog: { height: 44 },
  menu: { width: 245, height: 48 }, // procijenjeno sa slike, provjeriti u Figmi (Inspect)
};

const MIN_TOUCH = 48;

// Proširenje površine za dodir (oko dugmeta, nevidljivo) do MIN_TOUCH px visine.
const touchArea = (height) => {
  const extra = Math.max(0, Math.ceil((MIN_TOUCH - height) / 2));
  return extra > 0 ? { top: extra, bottom: extra, left: 8, right: 8 } : undefined;
};

// Široko dugme: širina roditelja, najviše širina iz Figme.
const wide = (size) => ({
  width: '100%',
  maxWidth: size.width,
  minHeight: size.height,
  paddingHorizontal: spacing.px16,
});

// Zeleno dugme sa tamnim obrubom (Prijavi se, Informacije, Sign In)
const primaryContainer = {
  flexDirection: 'row',
  alignItems: 'center',
  justifyContent: 'center',
  backgroundColor: colors.primary,
  borderColor: colors.gray800,
  borderWidth: 1,
  borderRadius: borderRadius.px8,
  padding: spacing.px12,
  gap: spacing.px8,
};

const primaryLabel = {
  fontFamily: fonts.inter400,
  fontSize: 16,
  letterSpacing: 0,
  color: colors.background,
};

// Mali zaobljeni "chip" (Recenzije, Objave, Grupe, filteri)
const chipContainer = {
  flexDirection: 'row',
  alignItems: 'center',
  justifyContent: 'center',
  backgroundColor: colors.surface,
  borderWidth: 0,
  borderRadius: borderRadius.filter,
  paddingTop: spacing.filterPaddingTop,
  paddingBottom: spacing.filterPaddingBottom,
  paddingLeft: spacing.filterPaddingHorizontal,
  paddingRight: spacing.filterPaddingHorizontal,
  gap: spacing.filterGap,
};

const chipLabel = {
  fontFamily: fonts.spartan400,
  fontSize: 12,
  letterSpacing: -0.06,
  color: colors.primary,
};

const chipHitSlop = touchArea(buttonSizes.chip.height);

// Široko ljubičasto-sivo dugme (Izmijeni lozinku, Kreiraj, Prijavi problem)
const actionContainer = {
  flexDirection: 'row',
  alignItems: 'center',
  justifyContent: 'center',
  backgroundColor: colors.surface,
  borderRadius: borderRadius.px10,
  borderWidth: 0,
};

const actionLabel = {
  fontFamily: fonts.montserrat600,
  fontSize: 20,
  color: colors.primary,
};

// ---------------------------------------------------------------------
// Varijante
// Svaka ima container (stil dugmeta), label (stil teksta) i,
// kod malih dugmadi, hitSlop (dodatna površina za dodir).
// ---------------------------------------------------------------------

export const buttons = {
  // Prijavi se. Veličina prema tekstu, najmanje kao u Figmi.
  primary: {
    container: {
      ...primaryContainer,
      minWidth: buttonSizes.primary.width,
      minHeight: buttonSizes.primary.height,
    },
    label: { ...primaryLabel },
  },

  // Informacije
  information: {
    container: {
      ...primaryContainer,
      minWidth: buttonSizes.information.width,
      minHeight: buttonSizes.information.height,
    },
    label: {
      ...primaryLabel,
      color: colors.gray950,
    },
  },

  // Sign In (široko)
  login: {
    container: {
      ...primaryContainer,
      ...wide(buttonSizes.login),
      paddingVertical: spacing.px8, // Figma: 42 px visine sa slovima od 18 px
    },
    label: {
      ...primaryLabel,
      fontFamily: fonts.actor400,
      fontSize: 18,
    },
    hitSlop: touchArea(buttonSizes.login.height),
  },

  // Izmijeni lozinku, Kreiraj, Prijavi problem (široko)
  action: {
    container: {
      ...actionContainer,
      ...wide(buttonSizes.action),
    },
    label: { ...actionLabel },
    hitSlop: touchArea(buttonSizes.action.height),
  },

  // Dodaj grupu (široko)
  addGroup: {
    container: {
      ...actionContainer,
      ...wide(buttonSizes.addGroup),
    },
    label: {
      ...actionLabel,
      color: colors.textLight,
    },
  },

  // Dodaj (široko, zaobljeno)
  add: {
    container: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.primary,
      borderWidth: 0,
      borderRadius: borderRadius.px29,
      ...wide(buttonSizes.add),
    },
    label: {
      fontFamily: fonts.inter500,
      fontSize: 18,
      lineHeight: 26,
      color: colors.surface,
    },
  },

  // Recenzije, Objave, Grupe. Izgled kao u Figmi, dodir proširen preko hitSlop.
  chip: {
    container: { ...chipContainer },
    label: { ...chipLabel },
    hitSlop: chipHitSlop,
  },

  chipSelected: {
    container: {
      ...chipContainer,
      backgroundColor: colors.primary,
    },
    label: {
      ...chipLabel,
      color: colors.background,
    },
    hitSlop: chipHitSlop,
  },

  chipMedium: {
    container: { ...chipContainer },
    label: {
      ...chipLabel,
      fontFamily: fonts.spartan500,
    },
    hitSlop: chipHitSlop,
  },

  // Zaprati i odabrana varijanta Grupe
  chipSelectedMedium: {
    container: {
      ...chipContainer,
      backgroundColor: colors.primary,
    },
    label: {
      ...chipLabel,
      fontFamily: fonts.spartan500,
      color: colors.background,
    },
    hitSlop: chipHitSlop,
  },

  // Prijavite problem ispod forme za prijavu
  textLink: {
    container: {
      backgroundColor: 'transparent',
      borderWidth: 0,
      padding: 0,
      flexDirection: 'row',
      alignItems: 'center',
    },
    label: {
      fontFamily: fonts.actor400,
      fontSize: 18,
      lineHeight: 25.2,
      color: colors.primary,
      textDecorationLine: 'underline',
    },
    hitSlop: touchArea(buttonSizes.textLink.height),
  },

  // Odustani (dugme u dijalogu)
  secondary: {
    container: {
      ...primaryContainer,
      backgroundColor: 'transparent',
      borderColor: colors.border,
      minHeight: buttonSizes.dialog.height,
      paddingVertical: spacing.px8,
      paddingHorizontal: spacing.px16,
    },
    label: {
      ...primaryLabel,
      color: colors.textLight,
    },
  },

  // Potvrdi (npr. Odjavi se)
  confirm: {
    container: {
      ...primaryContainer,
      minHeight: buttonSizes.dialog.height,
      paddingVertical: spacing.px8,
      paddingHorizontal: spacing.px16,
    },
    label: { ...primaryLabel },
  },

  // Obriši (brisanje člana, trenera, plana, termina…)
  danger: {
    container: {
      ...primaryContainer,
      backgroundColor: colors.accentRed,
      borderColor: colors.accentRed,
      minHeight: buttonSizes.dialog.height,
      paddingVertical: spacing.px8,
      paddingHorizontal: spacing.px16,
    },
    label: {
      ...primaryLabel,
      color: colors.textPrimary,
    },
  },

  // Početni meni radnika i administratora (Pregled svih trenera, Obrada problema...)
  menu: {
    container: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.surface,
      borderColor: colors.border,
      borderWidth: 1,
      borderRadius: borderRadius.px8,
      ...wide(buttonSizes.menu),
    },
    label: {
      fontFamily: fonts.inter600,
      fontSize: 16,
      color: colors.textLight,
    },
  },
};

export default buttons;
