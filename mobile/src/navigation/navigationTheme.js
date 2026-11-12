import { DarkTheme } from '@react-navigation/native';
import { theme } from '../theme';

const { colors } = theme;

export const navigationTheme = { 
    ...DarkTheme,
    colors: {
        ...DarkTheme.colors,
        primary: colors.primary,
        background: colors.background,
        card: colors.background,
        text: colors.textPrimary,
        border: colors.surface,
        notification: colors.accentRed,
    },
};