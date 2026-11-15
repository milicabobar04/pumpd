import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import LoginScreen from '../screens/auth/LoginScreen';
import GymInfoScreen from '../screens/public/GymInfoScreen';
import ReportIssueScreen from '../screens/public/ReportIssueScreen';

const Stack = createNativeStackNavigator();

// Ekrani dostupni BEZ prijave.
export default function AuthStack() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="GymInfo" component={GymInfoScreen} options={{ animation: 'slide_from_right' }} />
      <Stack.Screen name="ReportIssue" component={ReportIssueScreen} options={{ animation: 'slide_from_right' }} />
    </Stack.Navigator>
  );
}
