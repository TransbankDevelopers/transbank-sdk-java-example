function HttpUtil() {

    this.getHttpRequest = function () {
        if (window.XMLHttpRequest) {return new XMLHttpRequest();}
        // eslint-disable-next-line no-undef
        return ActiveXObject('Microsoft.XMLHTTP');
    };

    this.sendPostRedirect = function (destination, params) {
        var form = document.createElement('form');

        form.method = 'POST';
        form.action = destination;

        Object.keys(params).forEach(function (key) {
            var param = document.createElement('input');

            param.type = 'hidden';
            param.name = key;
            param.value = params[key];
            form.appendChild(param);
        });

        var submit = document.createElement('input');

        submit.type = 'submit';
        submit.name = 'submitButton';
        submit.style.display = 'none';

        form.appendChild(submit);
        document.body.appendChild(form);
        form.submit();
    };

    this.sendGetRedirect = function (destination, params) {
        var keys = Object.keys(params);
        var urlParams = keys.map(function (param) {
            return encodeURIComponent(param) + '=' + encodeURIComponent(params[param]);
        }).join('&');
        window.location = destination + '?' + urlParams;
    };
}
