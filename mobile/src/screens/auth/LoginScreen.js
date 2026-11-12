import React from 'react';
import { Text, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '../../context/AuthContext';
import { Button, Card } from '../../components';
import { theme, cardSizes } from '../../theme';

// privremeno do faze 1, kasnije zamijeniti sa pravom formom
const DEV_USERS = [
  { id: 1, name: 'Test', surname: 'Član', email: 'clan@pumpd.test', role: 'MEMBER' },
  { id: 2, name: 'Test', surname: 'Trener', email: 'trener@pumpd.test', role: 'TRAINER' },
  { id: 3, name: 'Test', surname: 'Radnik', email: 'radnik@pumpd.test', role: 'WORKER' },
  { id: 4, name: 'Test', surname: 'Admin', email: 'admin@pumpd.test', role: 'ADMIN' },
];

const ROLE_LABELS = {
  MEMBER: 'Uđi kao član',
  TRAINER: 'Uđi kao trener',
  WORKER: 'Uđi kao radnik',
  ADMIN: 'Uđi kao administrator',
};

export default function LoginScreen() {
  const { signIn } = useAuth();

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>pumpd</Text>
      <Text style={styles.caption}>Razvojni ulaz – izaberi ulogu</Text>

      <Card variant="loginPanel" style={styles.panel}>
        {DEV_USERS.map((u) => (
          <Button
            key={u.role}
            title={ROLE_LABELS[u.role]}
            variant="login"
            onPress={() => signIn({ ...u, token: 'dev-token' })}
          />
        ))}
      </Card>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    padding: theme.spacing.px24,
    gap: theme.spacing.px16,
  },
  title: {
    ...theme.typography.screenTitle,
    color: theme.colors.primary,
    fontSize: 32,
  },
  caption: theme.typography.caption,
  panel: {
    width: '100%',
    maxWidth: cardSizes.loginPanel.width,
    alignItems: 'center',
  },
});