jQuery.fn.outerHTML = function() {
  return jQuery('<div />').append(this.eq(0).clone()).html();
};

function hex2a(hexx) {
    var hex = hexx.toString();//force conversion
    var str = '';
    for (var i = 0; i < hex.length; i += 2)
        str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
    return str;
}

function hex2arrayBuffer(data) {
  var length = data.length / 2;
  var ret = new Uint8Array(length);
  for (var i = 0; i < length; ++i) {
    ret[i] = parseInt(data.substr(i * 2, 2), 16);
  }
  return ret.buffer;
}


$(document).ready(function() {
    $('body').popover({
        selector: '.has-popover',
        trigger: 'hover focus',
        html: true,
       // template: '<div class="popover" role="tooltip"><div class="arrow"></div><h3 class="popover-title"></h3><code><div class="popover-content"></div></code></div>',
        content: function() {
            var payload = $(this).data('payload');
            if ($(this).hasClass('cbor-payload')) {
                try {
                    var elem = $('<pre></pre>').text(
                        JSON.stringify( CBOR.decode( hex2arrayBuffer(payload) ), null, 4 )
                    );
                    return elem.outerHTML();
                } catch(e) {
                    return "<em>Not a CBOR document</em>"
                }
            } 

            if ($(this).hasClass('text-payload')) {
                return hex2a(payload);
            }

            return payload;

        }
    });


    var updateUI = function() {
        $.ajax({
                url: '/sigfox/api',
                type: 'GET',
                dataType: 'json',
            })
            .done(function(data) {

                // make this smarter (ie do not clear the whole list every time)
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
                        html: '<button type="button" class="btn btn-info btn-xs has-popover cbor-payload" data-title="CBOR" data-payload="' + data[i].payload + '" data-placement="top">CBOR</button> &nbsp;' +
                                '<button type="button" class="btn btn-info btn-xs has-popover text-payload" data-title="text" data-payload="' + data[i].payload + '" data-placement="top">text</button>'

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
            })
            .always(function() {
                setTimeout(updateUI, 2000);
            });
    }

    updateUI();

});
