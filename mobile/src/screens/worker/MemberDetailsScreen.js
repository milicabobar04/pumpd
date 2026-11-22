import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, ScrollView, ActivityIndicator, StyleSheet } from 'react-native';
import { Button, ConfirmDialog, EmptyState, ErrorMessage } from '../../components';
import { theme } from '../../theme';
import { getMember, deleteMember } from '../../services/membersService';
import { formatDate } from '../../utils/date';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const orDash = (value, unit = '') => (value === null || value === undefined ? '–' : `${value}${unit}`);

const InfoRow = ({ label, value }) => (
  <View style={styles.row}>
    <Text style={styles.rowLabel} maxFontSizeMultiplier={MAX_FONT_SCALE}>{label}</Text>
    <Text style={styles.rowValue} maxFontSizeMultiplier={MAX_FONT_SCALE}>{value}</Text>
  </View>
);

export default function MemberDetailsScreen({ navigation, route }) {
  const { memberId } = route.params;

  const [member, setMember] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [notFound, setNotFound] = useState(false); 
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const load = useCallback(async () => {
    setLoadError(null);
    try {
      setMember(await getMember(memberId));
    } catch (e) {
      if (e.response?.status === 404) 
        setNotFound(true);
      else 
        setLoadError(e);
    } finally {
      setLoading(false);
    }
  }, [memberId]);

  useEffect(() => {
    load();
  }, [load]);

  const remove = async () => {
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteMember(memberId);
      setConfirmOpen(false);
      navigation.goBack(); 
    } catch (e) {
      setConfirmOpen(false);
      if (e.response?.status === 404) {
        navigation.goBack(); 
      } else {
        setDeleteError(e);
      }
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje člana" />
      </View>
    );
  }

  if (notFound) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <EmptyState
          icon="person-remove-outline"
          title="Član više ne postoji"
          message="Ovaj član je uklonjen."
        />
        <Button title="Nazad na listu" variant="action" onPress={() => navigation.goBack()} />
      </View>
    );
  }

  if (!member) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={loadError} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  const fullName = `${member.name} ${member.surname}`;
  const membership = member.membershipActive
    ? `Aktivna do ${formatDate(member.membershipValidUntil)}`
    : 'Nema aktivnu članarinu';

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {fullName}
        </Text>

        <View style={styles.card}>
          <InfoRow label="Email" value={member.email} />
          <InfoRow label="Godina rođenja" value={orDash(member.birthYear)} />
          <InfoRow label="Težina" value={orDash(member.weight, ' kg')} />
          <InfoRow label="Visina" value={orDash(member.height, ' cm')} />
          <InfoRow label="Članarina" value={membership} />
        </View>

        <ErrorMessage error={deleteError} inline />

        <Button
          title="Članarina"
          variant="action"
          onPress={() => navigation.navigate('WorkerMemberMembership', { memberId, memberName: fullName })}
          style={styles.deleteButton}
        />

        <Button
          title="Ukloni člana"
          variant="danger"
          onPress={() => setConfirmOpen(true)}
          style={styles.deleteButton}
        />
      </ScrollView>

      <ConfirmDialog
        visible={confirmOpen}
        title="Uklanjanje člana"
        message={`Da li ste sigurni da želite ukloniti člana ${fullName}? Ova radnja se ne može poništiti.`}
        confirmLabel="Ukloni"
        confirmVariant="danger"
        loading={deleting}
        onConfirm={remove}
        onCancel={() => setConfirmOpen(false)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  centered: { alignItems: 'center', justifyContent: 'center' },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px16 },
  title: { ...typography.screenTitle, color: colors.primary },
  card: {
    gap: spacing.px12,
    padding: spacing.px16,
    borderRadius: borderRadius.px10,
    backgroundColor: colors.surface,
  },
  row: { gap: spacing.px2 },
  rowLabel: { ...typography.caption },
  rowValue: { ...typography.body },
  deleteButton: { alignSelf: 'center', marginTop: spacing.px8 },
});