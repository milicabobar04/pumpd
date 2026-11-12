import React from 'react';
import { View, TouchableOpacity } from 'react-native';
import { cards } from '../theme/Cards';


export const Card = ({ variant, onPress, style, children, accessibilityLabel }) => {
  const s = cards[variant] || cards.exercise;

  if (onPress) {
    return (
      <TouchableOpacity
        style={[s.container, style]}
        onPress={onPress}
        activeOpacity={0.8}
        accessibilityRole="button"
        accessibilityLabel={accessibilityLabel}
      >
        {children}
      </TouchableOpacity>
    );
  }

  return <View style={[s.container, style]}>{children}</View>;
};