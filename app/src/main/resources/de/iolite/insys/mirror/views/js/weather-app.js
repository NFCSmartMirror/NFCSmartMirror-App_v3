var app = angular.module('weatherApp', []);
app.controller('weatherCtrl', function($scope, $http, $timeout) {

	var updateCurrentWeather = function(data) {
		$scope.weather.current = data;
		if(data === null) {
			return;
		}
		var date = new Date($scope.weather.current.sunriseTimestamp);
		$scope.weather.current.sunriseReadable = date.getHours()+":"+("0" + date.getMinutes()).slice(-2);
		date = new Date($scope.weather.current.sunsetTimestamp);
		$scope.weather.current.sunsetReadable = date.getHours()+":"+("0" + date.getMinutes()).slice(-2);

		var isNight = $scope.weather.current.timeOfDay == "Night"
		var icon = isNight ? "clear-night" : "clear-day";
		if($scope.weather.current.cloudiness > 30) icon = isNight ? "partly-cloudy-night" : "partly-cloudy-day";
		if($scope.weather.current.cloudiness > 70) icon = "cloudy";
		if($scope.weather.current.fog) icon = "fog";
		if($scope.weather.current.windSpeed > 10) icon = "wind";
		if($scope.weather.current.rainIntensity == "Light" || $scope.weather.current.rainIntensity == "Regular") icon = "rain";
		if($scope.weather.current.rainIntensity == "Heavy") icon = "sleet";
		if($scope.weather.current.snowIntensity != null && $scope.weather.current.snowIntensity != "None") icon = "snow";
		$scope.weather.current.statusIcon = icon;
	};

	var poll = function() {
		$http.get('current-weather.json').then(function(r) {
			console.log(r.data);
			updateCurrentWeather(r.data);
			$timeout(poll, 5000);
		});
	};
	poll();

	$scope.weather = {'current': null, 'forecast': null};

});
