import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Button, ErrorMessage, FormField } from '../../components';
import { theme } from '../../theme';
import { getGymInfo, updateGymInfo } from '../../services/gymService';
import { onlyErrors } from '../../utils/validation';
import { formatDate, parseDate, todayIso } from '../../utils/date';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

// Ograničenja iz GymInfoUpdate (openapi 5.2)
const MAX_NAME = 45;
const MAX_DESCRIPTION = 500;
const MAX_WORKING_HOURS = 45;

const EMPTY_FORM = {
  name: '',
  description: '',
  membershipPrice: '',
  dateOfEstablishment: '',
  workingHours: '',
};

// Poruka sa servera, osim generičke "string" koju vraća Prism mock (primjer iz šeme).
const serverMessage = (message) =>
  typeof message === 'string' && message.trim() && message !== 'string' ? message : null;

// Podaci sa servera -> tekst u poljima forme
function toForm(info) {
  const price = info?.membershipPrice;
  return {
    name: info?.name ?? '',
    description: info?.description ?? '',
    membershipPrice:
      price === null || price === undefined || price === '' ? '' : String(price).replace('.', ','),
    dateOfEstablishment: info?.dateOfEstablishment ? formatDate(info.dateOfEstablishment) : '',
    workingHours: info?.workingHours ?? '',
  };
}

function requiredText(value, label, max) {
  const v = value.trim();
  if (!v) return `${label} je obavezno.`;
  if (v.length > max) return `${label} može imati najviše ${max} znakova.`;
  return undefined;
}

function validatePrice(value) {
  const v = value.trim().replace(',', '.');
  if (!v) return 'Cijena članarine je obavezna.';
  const n = Number(v);
  if (Number.isNaN(n)) return 'Cijena članarine mora biti broj.';
  if (n <= 0) return 'Cijena članarine mora biti veća od 0.';
  return undefined;
}

function validateDate(value) {
  if (!value.trim()) return 'Datum osnivanja je obavezan.';
  const iso = parseDate(value);
  if (!iso) return 'Datum mora biti u formatu dd.mm.gggg.';
  if (iso > todayIso()) return 'Datum osnivanja ne smije biti u budućnosti.';
  return undefined;
}

function validate(form) {
  return onlyErrors({
    name: requiredText(form.name, 'Naziv', MAX_NAME),
    description: requiredText(form.description, 'Opis', MAX_DESCRIPTION),
    membershipPrice: validatePrice(form.membershipPrice),
    dateOfEstablishment: validateDate(form.dateOfEstablishment),
    workingHours: requiredText(form.workingHours, 'Radno vrijeme', MAX_WORKING_HOURS),
  });
}

export default function EditGymInfoScreen() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [loaded, setLoaded] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [success, setSuccess] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      setForm(toForm(await getGymInfo()));
      setLoaded(true);
    } catch (e) {
      setLoadError(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const setField = (key) => (value) => {
    setForm((f) => ({ ...f, [key]: value }));
    if (errors[key]) setErrors((e) => ({ ...e, [key]: undefined }));
    setSuccess(null);
  };

  const save = async () => {
    const found = validate(form);
    setErrors(found);
    setSuccess(null);
    setSaveError(null);
    if (Object.keys(found).length > 0) return;

    setSaving(true);
    try {
      const result = await updateGymInfo({
        name: form.name.trim(),
        description: form.description.trim(),
        membershipPrice: Number(form.membershipPrice.trim().replace(',', '.')),
        dateOfEstablishment: parseDate(form.dateOfEstablishment),
        workingHours: form.workingHours.trim(),
      });
      setSuccess(serverMessage(result?.message) || 'Informacije o teretani su sačuvane.');
    } catch (e) {
      setSaveError(e);
    } finally {
      setSaving(false);
    }
  };

  if (loading && !loaded) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje informacija o teretani" />
      </View>
    );
  }

  if (!loaded) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={loadError} onRetry={load} />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView style={styles.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
          Informacije o teretani
        </Text>

        <FormField
          label="Naziv"
          value={form.name}
          onChangeText={setField('name')}
          error={errors.name}
          editable={!saving}
          maxLength={MAX_NAME}
        />
        <FormField
          label="Opis"
          value={form.description}
          onChangeText={setField('description')}
          error={errors.description}
          editable={!saving}
          maxLength={MAX_DESCRIPTION}
          multiline
          numberOfLines={5}
          inputStyle={styles.description}
        />
        <FormField
          label="Cijena članarine (KM)"
          value={form.membershipPrice}
          onChangeText={setField('membershipPrice')}
          error={errors.membershipPrice}
          editable={!saving}
          keyboardType="decimal-pad"
          placeholder="npr. 50,00"
        />
        <FormField
          label="Datum osnivanja"
          value={form.dateOfEstablishment}
          onChangeText={setField('dateOfEstablishment')}
          error={errors.dateOfEstablishment}
          editable={!saving}
          keyboardType="numbers-and-punctuation"
          placeholder="dd.mm.gggg."
        />
        <FormField
          label="Radno vrijeme"
          value={form.workingHours}
          onChangeText={setField('workingHours')}
          error={errors.workingHours}
          editable={!saving}
          maxLength={MAX_WORKING_HOURS}
          placeholder="npr. Pon–Pet 07–22, Sub 09–15"
        />

        <ErrorMessage error={saveError} inline />

        {success ? (
          <View style={styles.success} accessibilityRole="alert" accessibilityLiveRegion="polite">
            <Ionicons name="checkmark-circle-outline" size={20} color={colors.primary} />
            <Text style={styles.successText} maxFontSizeMultiplier={MAX_FONT_SCALE}>
              {success}
            </Text>
          </View>
        ) : null}

        <Button title="Sačuvaj izmjene" variant="action" onPress={save} loading={saving} style={styles.submit} />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  centered: { alignItems: 'center', justifyContent: 'center' },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px16 },
  title: { ...typography.screenTitle, color: colors.primary },
  description: { minHeight: 110 },
  success: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px8,
    padding: spacing.px12,
    backgroundColor: colors.surface,
    borderColor: colors.primary,
    borderWidth: 1,
    borderRadius: borderRadius.px8,
  },
  successText: { ...typography.body, flex: 1 },
  submit: { alignSelf: 'center', marginTop: spacing.px8 },
});
