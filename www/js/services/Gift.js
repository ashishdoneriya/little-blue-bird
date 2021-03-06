
angular.module('Gift', ['ngResource']).
  factory('Gift', function($resource) {
      var Gift = $resource('http://www.littlebluebird.com/gf/rest/gifts/:giftId/:updater', {giftId:'@giftId', updater:'@updater', viewerId:'@viewerId', recipientId:'@recipientId', recipients:'@recipients', circleId:'@circleId', description:'@description', url:'@url', addedBy:'@addedBy', status:'@status', senderId:'@senderId', senderName:'@senderName', reallyWants:'@reallyWants', deleted:'@deleted', urlAff:'@urlAff', affiliateId:'@affiliateId', receivedate:'@receivedate'}, 
                    {
                      query: {method:'GET', isArray:true}, 
                      delete: {method:'DELETE'},
                      save: {method:'POST'},
                    });
                    
                    
      //2013-08-26
      // product is a barcode-scanned product              
      Gift.convertProductToGift = function(product, circle, user) {
        var gift = {addedBy:user.id};
	    if(angular.isDefined(circle))
	      gift.circle = circle;
		gift.description = product.name;
		gift.url = product.url;
		gift.affiliateUrl = product.url;
		gift.canedit = true;
	    console.log('Gift.convertProductToGift: gift=', JSON.stringify(gift))
		return gift;
      }
      
      
      
      //2013-08-26
      Gift.addrecipient = function(parms) {
          var recipient = parms.recipient;
          var gift = parms.gift;
          var user = parms.user;
          var saveGiftSuccessFn = parms.saveGiftSuccessFn;
          
	      var parms2 = {gift:gift, recipients:[recipient], replace:false};
	      gift = Gift.setRecipients(parms2);
	      
	      console.log('Gift.setRecipients:', gift.recipients);
	      
	      // we need recipientId for gift.edbr on the server side
	      var saveparms = {giftId:gift.id, updater:user.fullname, description:gift.description, url:gift.url, 
               addedBy:gift.addedBy, recipientId:recipient.id, recipients:gift.recipients, viewerId:user.id, 
               senderId:gift.sender, senderName:gift.sender_name};
          
          // not even going to mess with whether there is a circle or not
          
	      savedgift = Gift.save(saveparms, 
	        function() {
	            console.log('Gift.addrecipient: got this savedgift', savedgift); // we do get this
	            saveGiftSuccessFn(savedgift);
	        }, 
	        function() {console.log("$scope.savegift_takingargs: FAIL FUNCTION")});
      }
      
      
      //2013-08-26
      Gift.setRecipients = function(parms) {
          var gift = parms.gift;
          var recipients = parms.recipients;
          var replace = parms.replace;
          
	      if(!angular.isDefined(gift.recipients))
	        gift.recipients = [];
	        
	      if(replace)
	        gift.recipients.splice(0, gift.recipients.length);
	          
	      for(var i=0; i < recipients.length; ++i) {
	        gift.recipients.push(recipients[i]);
	      }
	      
	      for(var i=0; i < gift.recipients.length; ++i) {
	        gift.recipients[i].checked = true;
	      } 
	      return gift;
      }
      
      
      //2013-08-26
      Gift.prepareAddRecipient = function(recipient, gift) {
	      if(!angular.isDefined(gift.recipients))
	        gift.recipients = [];
	      gift.recipients.push(recipient);
        
      }
      
      
      //2013-08-26
      Gift.addrecipients = function(parms) {
          var recipients = parms.recipients;
          var gift = parms.gift;
          var user = parms.user;
          var saveGiftSuccessFn = parms.saveGiftSuccessFn;
          
	      var parms2 = {gift:gift, recipients:recipients, replace:false};
          gift = Gift.setRecipients(parms2);
          
	      // recipientId not needed in this case because we don't return to a wishlist, we return to #recipients.  Since there's multiple recipients, we don't know whose list to return to
	      var saveparms = {giftId:gift.id, updater:user.fullname, description:gift.description, url:gift.url, 
               addedBy:gift.addedBy, recipients:gift.recipients, viewerId:user.id, 
               senderId:gift.sender, senderName:gift.sender_name};
          
	      savedgift = Gift.save(saveparms, 
	        function() {
	            console.log('Gift.addrecipients: got this savedgift', savedgift); // we do get this
	            saveGiftSuccessFn(savedgift);
	        }, 
	        function() {console.log("$scope.savegift_takingargs: FAIL FUNCTION")});
          
      }
      
      
      //2013-08-26
      Gift.removeRecipients = function(parms) {
        var deleteRecipients = parms.deleteRecipients;
        var gift = parms.gift;
        var updaterName = parms.updaterName;
        var viewerId = parms.viewerId;
        var successFn = parms.successFn;
    
	    for(j=0; j < deleteRecipients.length; ++j ) {
	      for(i=0; i < gift.recipients.length; ++i ) {
	        if(deleteRecipients[j].id == gift.recipients[i].id) {
	          console.log('at i='+i+' before splice, gift.recipients:', gift.recipients);
	          gift.recipients.splice(i, 1);
	          console.log('at i='+i+' after splice, gift.recipients:', gift.recipients);
	        }
	      }
	    }
	    
	    var parms2 = {gift:gift, recipients:angular.copy(gift.recipients), replace:true};
	    gift = Gift.setRecipients(parms2);
	    
	    // have to supply 'updaterName' otherwise RestService will think this call should be an insert
	    var savedgift = Gift.save({giftId:gift.id, recipients:gift.recipients, updater:updaterName, viewerId:viewerId}, function() {successFn(savedgift)});
    
      }

      return Gift;
  });