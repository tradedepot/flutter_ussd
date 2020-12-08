import 'package:flutter/material.dart';
import 'package:flutter_ussd/flutter_ussd.dart';
import 'dart:async';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();
  ValueNotifier<bool> _loading = ValueNotifier<bool>(false);
  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> dialUSSD() async {
    try {
      if (_loading.value) return;
      _loading.value = true;
      final response = await FlutterUssd.dial('*347*77*8#');
      _scaffoldKey.currentState.showSnackBar(
        SnackBar(
          content: Text(response),
        ),
      );
    } catch (error) {
      _scaffoldKey.currentState.showSnackBar(
        SnackBar(
          content: Text('Error dialing ussd: $error'),
        ),
      );
    } finally {
      _loading.value = false;
    }
  }

  Future<void> isSupported() async {
    try {
      final response = await FlutterUssd.isSupported();
      _scaffoldKey.currentState.showSnackBar(
        SnackBar(
          content: Text('USSD SUPPORTED: $response'),
        ),
      );
    } catch (error) {
      _scaffoldKey.currentState.showSnackBar(
        SnackBar(
          content: Text('Error checking support: $error'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        key: _scaffoldKey,
        appBar: AppBar(
          title: const Text('USSD Dial Sample'),
        ),
        body: Center(
            child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ValueListenableBuilder(
              builder: (context, loading, child) {
                return ElevatedButton.icon(
                    onPressed: loading ? null : dialUSSD,
                    icon: loading
                        ? SizedBox(
                            width: 24.0,
                            height: 24.0,
                            child: CircularProgressIndicator(),
                          )
                        : Icon(Icons.send),
                    label: Text('DIAL USSD'));
              },
              valueListenable: _loading,
            ),
            SizedBox(
              height: 24.0,
            ),
            TextButton(
              child: Text('IS SUPPORTED'),
              onPressed: isSupported,
            ),
          ],
        )),
      ),
    );
  }
}
