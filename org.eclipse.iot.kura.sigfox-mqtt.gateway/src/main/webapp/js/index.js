$(document).ready(function() {
    setInterval(function() {
        $.ajax({
                url: '/sigfox/api',
                type: 'GET',
                dataType: 'json',
            })
            .done(function(data) {

                $("#sigfox-messages tr").remove();

                for (var i = data.length - 1; i >= 0; i--) {
                    console.log(data[i]);
                    var row = $('<tr />');
                    var d = new Date();
                    d.setTime(data[i].timestamp * 1000);

                    $('<td />', {
                        text: d.toLocaleString()
                    }).appendTo(row);

                    $('<td />', {
                        text: data[i].deviceID
                    }).appendTo(row);

                    $('<td />', {
                        html: '<code>' + data[i].payload + '</code>'
                    }).appendTo(row);

                    $('<td />', {
                        text: data[i].RSSI
                    }).appendTo(row);

                    var label = (data[i].published ? '<span class="label label-success">COMPLETE</span>' :
                        '<span class="label label-info">PENDING</span>');
                    $('<td />', {
                        html: label
                    }).appendTo(row);


                    row.appendTo('#sigfox-messages');
                }

            })
            .fail(function(jqxhr, status, err) {
                console.log('AJAX error', jqxhr);
                //    })
                //    .always(function(jqxhr, status, err) {
                //      alert('Always');
            });
    }, 1000);
});
