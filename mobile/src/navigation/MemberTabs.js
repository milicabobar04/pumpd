import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import PlaceholderScreen from '../screens/common/PlaceholderScreen';
import ProfileScreen from '../screens/common/ProfileScreen';
import { tabScreenOptions, tabIcon } from './tabOptions';
import { MemberTrainingsStack } from './PlansStack';

const Tab = createBottomTabNavigator();


export default function MemberTabs() {
  return (
    <Tab.Navigator screenOptions={tabScreenOptions}>
      <Tab.Screen
        name="MemberHome"
        component={PlaceholderScreen}
        options={{ title: 'Početna', tabBarIcon: tabIcon('home-outline', 'home') }}
        initialParams={{ opis: 'Objave praćenih trenera i sačuvane objave', rute: '8.1–8.4', faza: 3 }}
      />
      <Tab.Screen
        name="MemberTrainings"
        component={MemberTrainingsStack}
        options={{
          title: 'Treninzi',
          headerShown: false,
          tabBarIcon: tabIcon('dumbbell', 'dumbbell', MaterialCommunityIcons),
        }}
      />
      <Tab.Screen
        name="MemberTrainers"
        component={PlaceholderScreen}
        options={{ title: 'Pretraga trenera', tabBarIcon: tabIcon('search-outline', 'search') }}
        initialParams={{ opis: 'Pretraga trenera, praćenje i recenzije', rute: '7.1–7.5', faza: 2 }}
      />
      <Tab.Screen
        name="MemberProfile"
        component={ProfileScreen}
        options={{ title: 'Profil', tabBarIcon: tabIcon('person-outline', 'person') }}
      />
      
    </Tab.Navigator>
  );
}
