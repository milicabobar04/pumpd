import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AppHeader } from '../components';
import TrainersListScreen from '../screens/member/TrainersListScreen';
import TrainerProfileScreen from '../screens/member/TrainerProfileScreen';
import TrainerReviewScreen from '../screens/member/TrainerReviewScreen';

const Stack = createNativeStackNavigator();

const screenOptions = {
  header: ({ navigation, back }) => <AppHeader navigation={navigation} showBack={!!back} />,
  animation: 'slide_from_right',
};

// Član: pretraga trenera (SU-11), profil sa recenzijama i praćenjem (SU-12, SU-13, SU-15),
// ostavljanje recenzije (SU-14)
export default function MemberTrainersStack() {
  return (
    <Stack.Navigator screenOptions={screenOptions}>
      <Stack.Screen name="MemberTrainersList" component={TrainersListScreen} options={{ title: 'Pretraga trenera' }} />
      <Stack.Screen name="MemberTrainerProfile" component={TrainerProfileScreen} options={{ title: 'Profil trenera' }} />
      <Stack.Screen name="MemberTrainerReview" component={TrainerReviewScreen} options={{ title: 'Recenzija' }} />
    </Stack.Navigator>
  );
}
