import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';

export default function App() {
  return (
    <View style={styles.container}>
      <Text style={styles.textbox}> Alizzzzz</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  textbox: {
    alignItems: 'flex-end',
    justifyContent: 'flex-end',
    color: '#941234',
    fontSize: 60,
  },
});
