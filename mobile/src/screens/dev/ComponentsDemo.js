import React, { useRef, useState } from 'react';
import { ScrollView, View, Text, StyleSheet } from 'react-native';
import { theme } from '../../theme';
import { Button } from '../../components/Button';
import {
  FormField,
  ListItem,
  EmptyState,
  ConfirmDialog,
  ErrorMessage,
} from '../../components';

const { colors, spacing, typography } = theme;

// Privremeni ekran za razvoj: prikazuje sve zajedničke komponente.
// Ne ide u navigaciju za korisnike.
export default function ComponentsDemo() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [dialog, setDialog] = useState(false);
  const [loading, setLoading] = useState(false);
  const passwordRef = useRef(null);

  const emailError = email && !email.includes('@') ? 'Unesi ispravnu email adresu.' : null;

  const confirm = () => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setDialog(false);
    }, 1500);
  };

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={typography.screenTitle}>Komponente</Text>

      <Text style={styles.section}>FormField</Text>
      <FormField
        label="Email"
        placeholder="ime@primjer.com"
        value={email}
        onChangeText={setEmail}
        error={emailError}
        keyboardType="email-address"
        autoCapitalize="none"
        returnKeyType="next"
        onSubmitEditing={() => passwordRef.current?.focus()}
      />
      <FormField
        ref={passwordRef}
        label="Lozinka"
        placeholder="Lozinka"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
      />
      <FormField label="Onemogućeno" value="Ne može se mijenjati" editable={false} />

      <Text style={styles.section}>ListItem</Text>
      <ListItem variant="person" title="Ana Petrović" onPress={() => {}} />
      <ListItem variant="person" title="Marko Jović" subtitle="Trener" />
      <ListItem variant="issue" title="Pokvarena traka za trčanje" onPress={() => {}} />
      <ListItem variant="exercise" title="Čučanj" subtitle="4 x 10" onPress={() => {}} />
      <ListItem variant="exerciseSelected" title="Mrtvo dizanje" subtitle="3 x 8" />
      <ListItem variant="exerciseBrown" title="Zgibovi" subtitle="3 x 12" />
      <ListItem variant="trainingGroup" title="Pilates" subtitle="Pon, Sri 18:00" onPress={() => {}} />

      <Text style={styles.section}>EmptyState</Text>
      <View style={styles.box}>
        <EmptyState
          title="Nema grupa"
          message="Još nisi dodala nijednu grupu."
          actionLabel="Dodaj"
          onAction={() => {}}
        />
      </View>

      <Text style={styles.section}>ErrorMessage</Text>
      <ErrorMessage error={{ status: 401, error: 'Unauthorized', message: 'Pogrešan email ili lozinka.' }} inline />
      <View style={styles.box}>
        <ErrorMessage error={{ request: {} }} onRetry={() => {}} />
      </View>

      <Text style={styles.section}>ConfirmDialog</Text>
      <Button title="Odjavi se" variant="danger" onPress={() => setDialog(true)} />
      <ConfirmDialog
        visible={dialog}
        title="Odjava"
        message="Da li želiš da se odjaviš?"
        confirmLabel="Odjavi se"
                confirmVariant="danger"
        loading={loading}
        onConfirm={confirm}
        onCancel={() => setDialog(false)}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    padding: spacing.px16,
    paddingTop: 56,
    paddingBottom: 48,
    gap: spacing.px12,
  },
  section: {
    ...typography.sectionTitle,
    marginTop: spacing.px16,
    color: colors.primary,
  },
  box: {
    minHeight: 260,
    borderWidth: 1,
    borderColor: colors.surface,
    borderRadius: 10,
  },
});
