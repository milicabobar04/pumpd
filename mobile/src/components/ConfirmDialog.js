import React from 'react';
import { Modal, View, Text, Pressable, StyleSheet } from 'react-native';
import { Button } from './Button';
import { theme } from '../theme';

const { colors, spacing, borderRadius, typography } = theme;

// Dijalog potvrde za radnje koje se ne mogu poništiti:
// odjava (SU-04), brisanje člana, trenera, plana, termina...
//
// Primjer:
//   <ConfirmDialog
//     visible={open}
//     title="Odjava"
//     message="Da li ste sigurni da želite da se odjavite?"
//     confirmLabel="Odjavi se"
//     confirmVariant="danger"
//     onConfirm={signOut}
//     onCancel={() => setOpen(false)}
//   />
export const ConfirmDialog = ({
  visible,
  title,
  message,
  confirmLabel = 'Potvrdi',
  cancelLabel = 'Odustani',
  confirmVariant = 'confirm', // 'confirm' (zeleno) ili 'danger' (crveno, za brisanje)
  loading = false, // dok traje API poziv: spinner na dugmetu, dijalog se ne može zatvoriti
  onConfirm,
  onCancel,
}) => {
  const close = loading ? undefined : onCancel;

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      statusBarTranslucent
      // Android: sistemsko dugme "nazad" zatvara dijalog kao "Odustani"
      onRequestClose={close}
    >
      <View style={styles.overlay}>
        {/* Dodir pored dijaloga (na zatamnjeni dio) = Odustani */}
        <Pressable
          style={StyleSheet.absoluteFill}
          onPress={close}
          accessibilityLabel={cancelLabel}
        />

        <View style={styles.dialog} accessibilityViewIsModal>
          <Text style={styles.title}>{title}</Text>
          {message ? <Text style={styles.message}>{message}</Text> : null}

          <View style={styles.actions}>
            <Button
              title={cancelLabel}
              variant="secondary"
              onPress={onCancel}
              disabled={loading}
              style={styles.action}
            />
            <Button
              title={confirmLabel}
              variant={confirmVariant}
              onPress={onConfirm}
              loading={loading}
              style={styles.action}
            />
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: colors.overlay,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.px24,
  },
  dialog: {
    width: '100%',
    maxWidth: 320,
    backgroundColor: colors.background,
    borderColor: colors.border,
    borderWidth: 1,
    borderRadius: borderRadius.px8,
    padding: spacing.px24,
    gap: spacing.px16,
  },
  title: {
    ...typography.sectionTitle,
  },
  message: {
    ...typography.body,
    color: colors.textSecondary,
  },
  actions: {
    flexDirection: 'row',
    gap: spacing.px12,
    marginTop: spacing.px8,
  },
  action: {
    flex: 1,
  },
});