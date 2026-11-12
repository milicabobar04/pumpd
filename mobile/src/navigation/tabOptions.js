import React from "react";
import { Ionicons } from '@expo/vector-icons';
import { theme } from '../theme';
import { AppHeader } from '../components';

const { colors, fonts } = theme;

export const tabScreenOptions = {
    header: ({ navigation }) => <AppHeader navigation={navigation} />,

    tabBarStyle: {
        backgroundColor: colors.surface,
        borderTopColor: colors.surface,
    },
    tabBarActiveTintColor: colors.primary,
    tabBarInactiveTintColor: colors.iconMuted,
    tabBarShowLabel: false,
    tabBarLabelStyle: {
        fontFamily: fonts.inter500,
        fontSize: 11
    },
};

export const tabIcon = (name, focusedName = name, IconSet = Ionicons) => ({
    focused, color, size
}) => (
    <IconSet name={focused ? focusedName : name} size={size} color={color}/>
);