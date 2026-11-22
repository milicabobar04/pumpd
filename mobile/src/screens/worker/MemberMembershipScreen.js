import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, ScrollView, ActivityIndicator, KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';
import { Button, ConfirmDialog, EmptyState, ErrorMessage, FormField } from '../../components';
import { theme } from '../../theme';
import { getMemberMembership, updateMemberMembership, extendMembership } from '../../services/membershipService';
import { formatDate, parseDate, todayIso } from '../../utils/date';
import { validateOptionalNumber, toNumberOrNull, onlyErrors } from '../../utils/validation';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;
const EXTEND_OPTIONS = [1, 3, 6, 12];

const monthsLabel = (n) => (n === 1 ? '1 mjesec' : n < 5 ? `${n} mjeseca` : `${n} mjeseci`);

const emptyForm = () => ({ paymentDate: formatDate(todayIso()), validUntil: '', price: '' });
const formFrom = (m) => ({
  paymentDate: formatDate(m.paymentDate),
  validUntil: formatDate(m.validUntil),
  price: String(m.price ?? ''),
});

const DATE_HINT = 'Unesite datum kao dd.mm.gggg.';

function validate(form) {
  const paymentDate = parseDate(form.paymentDate);
  const validUntil = parseDate(form.validUntil);
  let validUntilError;
  if (!form.validUntil.trim()) validUntilError = 'Datum isteka je obavezan.';
  else if (!validUntil) validUntilError = DATE_HINT;
  else if (paymentDate && validUntil < paymentDate) validUntilError = 'Datum isteka ne može biti prije datuma uplate.';

  return onlyErrors({
    paymentDate: !form.paymentDate.trim() ? 'Datum uplate je obavezan.' : !paymentDate ? DATE_HINT : undefined,
    validUntil: validUntilError,
    price: !form.price.trim()
      ? 'Cijena je obavezna.'
      : validateOptionalNumber(form.price, { label: 'Cijena', min: 0, max: 100000 }),
  });
}

const InfoRow = ({ label, value, valueStyle }) => (
  <View style={styles.row}>
    <Text style={styles.rowLabel} maxFontSizeMultiplier={MAX_FONT_SCALE}>{label}</Text>
    <Text style={[styles.rowValue, valueStyle]} maxFontSizeMultiplier={MAX_FONT_SCALE}>{value}</Text>
  </View>
);

export default function MemberMembershipScreen({ route }) {
  const { memberId, memberName } = route.params;

  const [membership, setMembership] = useState(null);
  const [missing, setMissing] = useState(false); 
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [notice, setNotice] = useState(null); // poruka o uspjehu
  const [extendMonths, setExtendMonths] = useState(null); // != null: otvoren dijalog

  const load = useCallback(async () => {
    setLoadError(null);
    try {
      setMembership(await getMemberMembership(memberId));
      setMissing(false);
    } catch (e) {
      if (e.response?.status === 404) {
        setMembership(null);
        setMissing(true);
      } else {
        setLoadError(e);
      }
    } finally {
      setLoading(false);
    }
  }, [memberId]);

  useEffect(() => {
    load();
  }, [load]);

  const startEdit = () => {
    setForm(membership ? formFrom(membership) : emptyForm());
    setErrors({});
    setActionError(null);
    setNotice(null);
    setEditing(true);
  };

  const setField = (key) => (value) => {
    setForm((f) => ({ ...f, [key]: value }));
    if (errors[key]) setErrors((e) => ({ ...e, [key]: undefined }));
  };

  const save = async () => {
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setSaving(true);
    setActionError(null);
    try {
      await updateMemberMembership(memberId, {
        paymentDate: parseDate(form.paymentDate),
        validUntil: parseDate(form.validUntil),
        price: toNumberOrNull(form.price),
      });
      setEditing(false);
      setNotice('Članarina je sačuvana.');
      await load();
    } catch (e) {
      setActionError(e);
    } finally {
      setSaving(false);
    }
  };

  const extend = async () => {
    setSaving(true);
    setActionError(null);
    setNotice(null);
    try {
      const result = await extendMembership(memberId, extendMonths);
      setNotice(`Članarina je produžena do ${formatDate(result.newValidUntil)}`);
      await load();
    } catch (e) {
      setActionError(e);
    } finally {
      setSaving(false);
      setExtendMonths(null);
    }
  };

  if (loading) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje članarine" />
      </View>
    );
  }

  if (loadError) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={loadError} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  let body;
  if (editing) {
    body = (
      <>
        <FormField label="Datum uplate" placeholder="dd.mm.gggg." value={form.paymentDate}
          onChangeText={setField('paymentDate')} error={errors.paymentDate} editable={!saving}
          keyboardType="numbers-and-punctuation" />
        <FormField label="Važi do" placeholder="dd.mm.gggg." value={form.validUntil}
          onChangeText={setField('validUntil')} error={errors.validUntil} editable={!saving}
          keyboardType="numbers-and-punctuation" />
        <FormField label="Cijena (KM)" value={form.price} onChangeText={setField('price')}
          error={errors.price} editable={!saving} keyboardType="decimal-pad" />
        <ErrorMessage error={actionError} inline />
        <Button title="Sačuvaj" variant="action" onPress={save} loading={saving} style={styles.centerButton} />
        <Button title="Odustani" variant="secondary" onPress={() => setEditing(false)} disabled={saving}
          style={styles.centerButton} />
      </>
    );
  } else if (missing) {
    body = (
      <EmptyState icon="card-outline" title="Nema članarine"
        message="Ovaj član još nema evidentiranu članarinu." actionLabel="Unesi članarinu" onAction={startEdit} />
    );
  } else {
    body = (
      <>
        <View style={styles.card}>
          <InfoRow label="Status" value={membership.isActive ? 'Aktivna' : 'Istekla'}
            valueStyle={membership.isActive ? styles.active : styles.inactive} />
          <InfoRow label="Datum uplate" value={formatDate(membership.paymentDate)} />
          <InfoRow label="Važi do" value={formatDate(membership.validUntil)} />
          <InfoRow label="Cijena" value={`${Number(membership.price).toFixed(2)} KM`} />
        </View>

        {notice ? <Text style={styles.notice} maxFontSizeMultiplier={MAX_FONT_SCALE}>{notice}</Text> : null}
        <ErrorMessage error={actionError} inline />

        <Text style={styles.sectionTitle} maxFontSizeMultiplier={MAX_FONT_SCALE}>Produži članarinu</Text>
        <View style={styles.options}>
          {EXTEND_OPTIONS.map((n) => (
            <Button key={n} title={monthsLabel(n)} variant="secondary" onPress={() => setExtendMonths(n)}
              disabled={saving} />
          ))}
        </View>

        <Button title="Uredi članarinu" variant="action" onPress={startEdit} style={styles.centerButton} />
      </>
    );
  }

  return (
    <KeyboardAvoidingView style={styles.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {memberName}
        </Text>
        {body}
      </ScrollView>

      <ConfirmDialog
        visible={extendMonths !== null}
        title="Produženje članarine"
        message={`Produžiti članarinu za ${monthsLabel(extendMonths ?? 1)}? Uplata se evidentira s današnjim datumom.`}
        confirmLabel="Produži"
        loading={saving}
        onConfirm={extend}
        onCancel={() => setExtendMonths(null)}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  centered: { alignItems: 'center', justifyContent: 'center' },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px16 },
  title: { ...typography.screenTitle, color: colors.primary },
  sectionTitle: { ...typography.sectionTitle },
  card: { gap: spacing.px12, padding: spacing.px16, borderRadius: borderRadius.px10, backgroundColor: colors.surface },
  row: { gap: spacing.px2 },
  rowLabel: { ...typography.caption },
  rowValue: { ...typography.body },
  active: { color: colors.primary },
  inactive: { color: colors.error },
  notice: { ...typography.body, color: colors.primary },
  options: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.px8 },
  centerButton: { alignSelf: 'center' },
});