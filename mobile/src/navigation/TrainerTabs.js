import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import PlaceholderScreen from '../screens/common/PlaceholderScreen';
import ProfileScreen from '../screens/common/ProfileScreen';
import { tabScreenOptions, tabIcon } from './tabOptions';

const Tab = createBottomTabNavigator();

export default function TrainerTabs() {
  return (
    <Tab.Navigator screenOptions={tabScreenOptions}>
      <Tab.Screen
        name="TrainerGroups"
        component={PlaceholderScreen}
        options={{ title: 'Moje grupe', tabBarIcon: tabIcon('people-outline', 'people') }}
        initialParams={{ opis: 'Grupe, članovi grupa i zahtjevi za prijavu', rute: '10.8–10.13', faza: 4 }}
      />
      <Tab.Screen
        name="TrainerPlans"
        component={PlaceholderScreen}
        options={{
          title: 'Planovi treninga',
          tabBarIcon: tabIcon('dumbbell', 'dumbbell', MaterialCommunityIcons),
        }}
        initialParams={{ opis: 'Planovi treninga', rute: '3.1–3.5', faza: 3 }}
      />
      <Tab.Screen
        name="TrainerSchedule"
        component={PlaceholderScreen}
        options={{ title: 'Raspored', tabBarIcon: tabIcon('calendar-outline', 'calendar') }}
        initialParams={{ opis: 'Dnevni raspored i termini', rute: '10.4–10.7', faza: 4 }}
      />
      <Tab.Screen
        name="TrainerProfile"
        component={ProfileScreen}
        options={{ title: 'Profil', tabBarIcon: tabIcon('person-outline', 'person') }}
      />
    </Tab.Navigator>
  );
}
