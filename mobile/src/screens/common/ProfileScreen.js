import React, { useCallback, useRef, useState } from 'react';
import { useFocusEffect } from '@react-navigation/native';
import {
  View,
  Text,
  ScrollView,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Button, FormField, ErrorMessage } from '../../components';
import { theme, cards, cardSizes } from '../../theme';
import { getMyProfile, updateMyProfile } from '../../services/userService';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const ROLE_LABELS = {
  MEMBER: 'Član',
  TRAINER: 'Trener',
  WORKER: 'Radnik',
  ADMIN: 'Administrator',
};

const EMPTY_VALUE = '—';
const MIN_BIRTH_YEAR = 1900;

const toText = (value) => (value === null || value === undefined ? '' : String(value));

const toNullableNumber = (text) => {
  const t = text.trim().replace(',', '.');
  return t === '' ? null : Number(t);
};

function validate(form) {
  const errors = {};
  const currentYear = new Date().getFullYear();

  if (!form.name.trim()) errors.name = 'Unesite ime.';
  if (!form.surname.trim()) errors.surname = 'Unesite prezime.';

  const year = form.birthYear.trim();
  if (year && (!/^\d{4}$/.test(year) || Number(year) < MIN_BIRTH_YEAR || Number(year) > currentYear)) {
    errors.birthYear = `Unesite godinu između ${MIN_BIRTH_YEAR} i ${currentYear}.`;
  }

  const weight = toNullableNumber(form.weight);
  if (weight !== null && !(Number.isFinite(weight) && weight > 0)) {
    errors.weight = 'Unesite težinu kao pozitivan broj.';
  }

  const height = toNullableNumber(form.height);
  if (height !== null && !(Number.isFinite(height) && height > 0)) {
    errors.height = 'Unesite visinu kao pozitivan broj.';
  }

  return errors;
}

const Stat = ({ value, unit, label }) => (
  <View style={styles.stat}>
    <Text style={cards.profileStats.value} maxFontSizeMultiplier={MAX_FONT_SCALE}>
      {value === null || value === undefined ? EMPTY_VALUE : `${value}${unit ? ` ${unit}` : ''}`}
    </Text>
    <Text style={cards.profileStats.label} maxFontSizeMultiplier={MAX_FONT_SCALE}>
      {label}
    </Text>
  </View>
);

const ProfileStats = ({ profile }) => (
  <View
    style={[cards.profileStats.container, styles.stats]}
    accessible
    accessibilityLabel={
      `Godina rođenja ${profile.birthYear ?? 'nije unesena'}, ` +
      `težina ${profile.weight ?? 'nije unesena'}, visina ${profile.height ?? 'nije unesena'}`
    }
  >
    <Stat value={profile.birthYear} label="Godina rođenja" />
    <View style={cards.profileStats.divider} />
    <Stat value={profile.weight} unit="kg" label="Težina" />
    <View style={cards.profileStats.divider} />
    <Stat value={profile.height} unit="cm" label="Visina" />
  </View>
);

const PasswordWarning = ({ onPress }) => (
  <View style={styles.warning} accessibilityRole="alert">
    <View style={styles.warningHeader}>
      <Ionicons name="warning-outline" size={24} color={colors.error} />
      <Text style={styles.warningTitle} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        Promijenite privremenu lozinku
      </Text>
    </View>
    <Text style={styles.warningText} maxFontSizeMultiplier={MAX_FONT_SCALE}>
      Vaš nalog koristi privremenu lozinku. Radi sigurnosti postavite svoju lozinku.
    </Text>
    <Button title="Promijeni lozinku" variant="primary" onPress={onPress} />
  </View>
);

function ProfileForm({ profile, onSaved, onCancel }) {
  const [form, setForm] = useState({
    name: profile.name ?? '',
    surname: profile.surname ?? '',
    birthYear: toText(profile.birthYear),
    weight: toText(profile.weight),
    height: toText(profile.height),
  });
  const [errors, setErrors] = useState({});
  const [saveError, setSaveError] = useState(null);
  const [saving, setSaving] = useState(false);

  const surnameRef = useRef(null);
  const birthYearRef = useRef(null);
  const weightRef = useRef(null);
  const heightRef = useRef(null);

  const setField = (key) => (value) => {
    setForm((f) => ({ ...f, [key]: value }));
    if (errors[key]) setErrors((e) => ({ ...e, [key]: undefined }));
  };

  const save = async () => {
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    const data = {
      name: form.name.trim(),
      surname: form.surname.trim(),
      birthYear: form.birthYear.trim() === '' ? null : Number(form.birthYear.trim()),
      weight: toNullableNumber(form.weight),
      height: toNullableNumber(form.height),
    };

    setSaving(true);
    setSaveError(null);
    try {
      await updateMyProfile(data);
      await onSaved();
    } catch (e) {
      setSaveError(e);
      setSaving(false);
    }
  };

  return (
    <View style={styles.form}>
      <Text style={typography.sectionTitle} accessibilityRole="header">
        Uređivanje profila
      </Text>

      <FormField
        label="Ime"
        value={form.name}
        onChangeText={setField('name')}
        error={errors.name}
        editable={!saving}
        autoCapitalize="words"
        returnKeyType="next"
        onSubmitEditing={() => surnameRef.current?.focus()}
      />
      <FormField
        ref={surnameRef}
        label="Prezime"
        value={form.surname}
        onChangeText={setField('surname')}
        error={errors.surname}
        editable={!saving}
        autoCapitalize="words"
        returnKeyType="next"
        onSubmitEditing={() => birthYearRef.current?.focus()}
      />
      <FormField
        ref={birthYearRef}
        label="Godina rođenja"
        placeholder="npr. 1998"
        value={form.birthYear}
        onChangeText={setField('birthYear')}
        error={errors.birthYear}
        editable={!saving}
        keyboardType="number-pad"
        maxLength={4}
        returnKeyType="next"
        onSubmitEditing={() => weightRef.current?.focus()}
      />
      <FormField
        ref={weightRef}
        label="Težina (kg)"
        placeholder="npr. 72.5"
        value={form.weight}
        onChangeText={setField('weight')}
        error={errors.weight}
        editable={!saving}
        keyboardType="decimal-pad"
        returnKeyType="next"
        onSubmitEditing={() => heightRef.current?.focus()}
      />
      <FormField
        ref={heightRef}
        label="Visina (cm)"
        placeholder="npr. 180"
        value={form.height}
        onChangeText={setField('height')}
        error={errors.height}
        editable={!saving}
        keyboardType="decimal-pad"
        returnKeyType="done"
        onSubmitEditing={save}
      />

      <ErrorMessage error={saveError} inline />

      <View style={styles.formActions}>
        <Button
          title="Odustani"
          variant="secondary"
          onPress={onCancel}
          disabled={saving}
          style={styles.formAction}
        />
        <Button
          title="Sačuvaj"
          variant="confirm"
          onPress={save}
          loading={saving}
          style={styles.formAction}
        />
      </View>
    </View>
  );
}

export default function ProfileScreen({ navigation }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [editing, setEditing] = useState(false);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      setProfile(await getMyProfile());
    } catch (e) {
      setLoadError(e);
    } finally {
      setLoading(false);
    }
  }, []);

  // Učitava profil svaki put kad ekran dobije fokus: pri prvom otvaranju,
  // povratku iz Podešavanja (npr. poslije promjene lozinke) i prelasku sa drugog taba.
  // Spinner se prikazuje samo dok profila još nema; kasnija osvježavanja idu "u pozadini".
  useFocusEffect(
    useCallback(() => {
      loadProfile();
    }, [loadProfile]),
  );

  const handleSaved = async () => {
    const fresh = await getMyProfile();
    setProfile(fresh);
    setEditing(false);
  };

  if (loading && !profile) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje profila" />
      </View>
    );
  }

  if (!profile) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={loadError} onRetry={loadProfile} />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {profile.mustChangePassword ? (
          <PasswordWarning onPress={() => navigation.navigate('Settings')} />
        ) : null}

        <View style={styles.panel}>
          <Ionicons name="person-circle-outline" size={88} color={colors.iconMuted} />

          <Text style={styles.name} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {profile.name} {profile.surname}
          </Text>
          <Text style={styles.email} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {profile.email}
          </Text>
          <Text style={styles.role} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {ROLE_LABELS[profile.role] ?? profile.role}
          </Text>

          <ProfileStats profile={profile} />
        </View>

        {editing ? (
          <ProfileForm
            profile={profile}
            onSaved={handleSaved}
            onCancel={() => setEditing(false)}
          />
        ) : (
          <Button title="Uredi profil" variant="action" onPress={() => setEditing(true)} />
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  centered: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    flexGrow: 1,
    alignItems: 'center',
    padding: spacing.px24,
    gap: spacing.px24,
  },

  panel: {
    width: '100%',
    alignItems: 'center',
    gap: spacing.px8,
    padding: spacing.px16,
    paddingBottom: spacing.px24,
    backgroundColor: colors.surface,
    borderRadius: borderRadius.px10,
  },
  name: {
    ...typography.screenTitle,
    textAlign: 'center',
  },
  email: {
    ...typography.body,
    color: colors.textSecondary,
    textAlign: 'center',
  },
  role: {
    ...typography.body,
    color: colors.primary,
    marginBottom: spacing.px8,
  },

  stats: {
    width: '100%',
    maxWidth: cardSizes.profileStats.width,
    minHeight: cardSizes.profileStats.height,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
    paddingVertical: spacing.px8,
  },
  stat: {
    flex: 1,
    alignItems: 'center',
    gap: spacing.px2,
  },

  warning: {
    width: '100%',
    gap: spacing.px12,
    padding: spacing.px16,
    backgroundColor: colors.surface,
    borderColor: colors.error,
    borderWidth: 1,
    borderRadius: borderRadius.px10,
  },
  warningHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px8,
  },
  warningTitle: {
    ...typography.sectionTitle,
    flex: 1,
  },
  warningText: {
    ...typography.body,
    color: colors.textSecondary,
  },

  form: {
    width: '100%',
    gap: spacing.px16,
  },
  formActions: {
    flexDirection: 'row',
    gap: spacing.px12,
    marginTop: spacing.px8,
  },
  formAction: {
    flex: 1,
  },
});
