ResultPage = {
	init: function(rootUrl) {
		ResultPage.rootUrl = rootUrl;
		Event.observe(window, "load", ResultPage.onLoad);
	},

	onLoad: function() {
		$$('.allResults>a').first().observe('click', ResultPage.onShowAllClicked);
		$('TestResultList').observe('click', ResultPage.onFailureTableClicked);
	},

	onShowAllClicked: function() {
		var url = window.location;
		new Ajax.Request(url+'allTests', {
			method: 'post',
			evalJS: 'false',
			onSuccess: function(t) {
				$$('.allResults').first().innerHTML = t.responseText;
			},
			onError: function(t) {
				console.log("Ajax Fail", t);
			}
		});
	},

	onFailureTableClicked: function(e) {
		if(e && e.target && e.target.hasClassName('showStackTrace')) {
			ResultPage.onStackTraceClicked(e, e.target);
		}
	},

	onStackTraceClicked: function(e, element) {
		var parent = element.up();
		var stackTraceElement = parent.down('code');
		if(stackTraceElement != null) {
			stackTraceElement.toggleClassName('hidden');
		} else {
			stackTraceElement = new Element("code");
			stackTraceElement.addClassName('stacktrace');
			stackTraceElement.innerText = "Loading...";
			parent.appendChild(stackTraceElement);

			var href = element.getAttribute('href');
			var url = window.location + href;
			new Ajax.Request(url, {
				method: 'post',
				evalJS: 'false',
				onSuccess: function(t) {
					stackTraceElement.innerText = t.responseText;
				},
				onError: function(t) {
					console.log("Ajax Fail", t);
				}
			});
		}
	}
};

