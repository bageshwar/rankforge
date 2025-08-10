/**
 * Demo functions for testing loading states
 * This file can be included to test various loading scenarios
 */

// Global loading state
window.RankForgeLoading = {
    // Test function to simulate API loading
    testAPICall: async function(duration = 2000) {
        console.log('🔄 Testing API call with loading state...');
        
        if (typeof showLoading === 'function') {
            showLoading('Testing API call...');
        }
        
        try {
            // Simulate API delay
            await new Promise(resolve => setTimeout(resolve, duration));
            
            console.log('✅ API call completed successfully');
            return { success: true, data: 'Mock data' };
        } catch (error) {
            console.error('❌ API call failed:', error);
            throw error;
        } finally {
            if (typeof hideLoading === 'function') {
                hideLoading();
            }
        }
    },
    
    // Test function to simulate navigation loading
    testNavigation: function(url, message = 'Navigating...') {
        console.log(`🔄 Testing navigation to: ${url}`);
        
        if (typeof showLoading === 'function') {
            showLoading(message);
        }
        
        // Simulate navigation delay
        setTimeout(() => {
            console.log(`✅ Navigation complete to: ${url}`);
            if (typeof hideLoading === 'function') {
                hideLoading();
            }
        }, 1500);
    },
    
    // Test function for section loading
    testSectionLoading: function(sectionSelector, duration = 1500) {
        console.log(`🔄 Testing section loading for: ${sectionSelector}`);
        
        const section = document.querySelector(sectionSelector);
        if (!section) {
            console.warn(`❌ Section not found: ${sectionSelector}`);
            return;
        }
        
        if (typeof showSectionLoading === 'function') {
            showSectionLoading(section);
        } else {
            section.style.opacity = '0.6';
            section.style.pointerEvents = 'none';
        }
        
        setTimeout(() => {
            console.log(`✅ Section loading complete for: ${sectionSelector}`);
            
            if (typeof hideSectionLoading === 'function') {
                hideSectionLoading(section);
            } else {
                section.style.opacity = '1';
                section.style.pointerEvents = 'auto';
            }
        }, duration);
    },
    
    // Show all available loading functions
    showAvailableFunctions: function() {
        console.log('🔍 Available loading functions:');
        console.log('- RankForgeLoading.testAPICall(duration)');
        console.log('- RankForgeLoading.testNavigation(url, message)');
        console.log('- RankForgeLoading.testSectionLoading(selector, duration)');
        console.log('- RankForgeLoading.showAvailableFunctions()');
        
        // Check what functions are available globally
        const globalFunctions = [];
        if (typeof showLoading === 'function') globalFunctions.push('showLoading()');
        if (typeof hideLoading === 'function') globalFunctions.push('hideLoading()');
        if (typeof showTableLoading === 'function') globalFunctions.push('showTableLoading()');
        if (typeof hideTableLoading === 'function') globalFunctions.push('hideTableLoading()');
        if (typeof showSectionLoading === 'function') globalFunctions.push('showSectionLoading()');
        if (typeof hideSectionLoading === 'function') globalFunctions.push('hideSectionLoading()');
        if (typeof makeAPICall === 'function') globalFunctions.push('makeAPICall()');
        if (typeof navigateWithLoading === 'function') globalFunctions.push('navigateWithLoading()');
        
        if (globalFunctions.length > 0) {
            console.log('🌐 Global loading functions:', globalFunctions.join(', '));
        } else {
            console.log('⚠️ No global loading functions detected on this page');
        }
    }
};

// Auto-log available functions when script loads
console.log('🚀 RankForge Loading Demo loaded!');
console.log('📖 Type RankForgeLoading.showAvailableFunctions() to see available functions');