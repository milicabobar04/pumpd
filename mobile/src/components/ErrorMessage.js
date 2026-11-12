import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { theme } from '../theme';
import { Button } from './Button';

const { colors, spacing, typography } = theme;
const MAX_FONT_SCALE = 1.3;

// Poruke kad server ne pošalje svoju poruku
const STATUS_MESSAGES = {
  400: 'Podaci nisu ispravni.',
  401: 'Sesija je istekla. Prijavi se ponovo.',
  403: 'Nemaš dozvolu za ovu akciju.',
  404: 'Traženi podatak nije pronađen.',
  409: 'Podatak već postoji.',
};
const SERVER_ERROR = 'Greška na serveru. Pokušaj ponovo kasnije.';
const NETWORK_ERROR = 'Nema veze sa serverom. Provjeri internet i pokušaj ponovo.';
const UNKNOWN_ERROR = 'Došlo je do greške.';

// Pretvara bilo koju grešku u tekst za korisnika.
// Prima:
//   - tekst: 'Pogrešna lozinka'
//   - grešku iz ugovora: { status: 401, error: 'Unauthorized', message: '...' }
//   - axios grešku (ima response ili request)
export function getErrorMessage(error) {
  if (!error) return null;
  if (typeof error === 'string') return error;

  // axios greška
  if (error.isAxiosError || error.response || error.request) {
    const data = error.response?.data;
    if (data && typeof data.message === 'string' && data.message) return data.message;
    if (!error.response) return NETWORK_ERROR;
    return messageForStatus(error.response.status);
  }

  // Greška u formatu ugovora ili običan Error
  if (typeof error.message === 'string' && error.message) return error.message;
  if (error.status) return messageForStatus(error.status);
  return UNKNOWN_ERROR;
}

const messageForStatus = (status) =>
  STATUS_MESSAGES[status] || (status >= 500 ? SERVER_ERROR : UNKNOWN_ERROR);

// Prikaz greške.
//
// inline   mala crvena poruka u redu (npr. iznad dugmeta na formi)
// inače    veći prikaz na sredini ekrana, sa dugmetom "Pokušaj ponovo" ako je zadat onRetry
//
// <ErrorMessage error={greska} onRetry={ucitaj} />
// <ErrorMessage error={greska} inline />
export const ErrorMessage = ({
  error,
  message,
  onRetry,
  retryLabel = 'Pokušaj ponovo',
  inline = false,
  style,
}) => {
  const text = message || getErrorMessage(error);
  if (!text) return null;

  if (inline) {
    return (
      <View style={[styles.inline, style]} accessibilityRole="alert" accessibilityLiveRegion="polite">
        <Ionicons name="alert-circle-outline" size={18} color={colors.error} />
        <Text style={styles.inlineText} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {text}
        </Text>
      </View>
    );
  }

  return (
    <View style={[styles.block, style]} accessibilityRole="alert" accessibilityLiveRegion="polite">
      <Ionicons name="alert-circle-outline" size={48} color={colors.error} />
      <Text style={styles.blockText} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {text}
      </Text>
      {onRetry ? <Button title={retryLabel} variant="secondary" onPress={onRetry} /> : null}
    </View>
  );
};

const styles = StyleSheet.create({
  inline: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px6,
  },
  inlineText: {
    ...typography.error,
    flex: 1,
  },
  block: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.px16,
    padding: spacing.px24,
  },
  blockText: {
    ...typography.body,
    textAlign: 'center',
    maxWidth: 300,
  },
});

export default ErrorMessage;
